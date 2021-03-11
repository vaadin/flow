/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
const fs = require('fs');
const mkdirp = require('mkdirp');
const path = require('path');

/**
 * This plugin handles minimization of the stats.json file to only contain required info to keep
 * the file as small as possible.
 *
 * In production mode the stats.json is written to the file system, in dev mode the json is returned to
 * the webpack callback function.
 */
class StatsPlugin {

  constructor(options = {}) {
    this.options = options;
  }

  apply(compiler) {
    const logger = compiler.getInfrastructureLogger("FlowIdPlugin");

    compiler.hooks.afterEmit.tapAsync("FlowIdPlugin", (compilation, done) => {
      let statsJson = compilation.getStats().toJson();
      // Get bundles as accepted keys
      let acceptedKeys = statsJson.assets.filter(asset => asset.chunks.length > 0)
        .map(asset => asset.chunks).reduce((acc, val) => acc.concat(val), []);

      // Collect all modules for the given keys
      const modules = collectModules(statsJson, acceptedKeys);

      // Collect accepted chunks and their modules
      const chunks = collectChunks(statsJson, acceptedKeys);

      let customStats = {
        hash: statsJson.hash,
        assetsByChunkName: statsJson.assetsByChunkName,
        chunks: chunks,
        modules: modules
      };

      if (!this.options.devMode) {
        // eslint-disable-next-line no-console
        logger.info("Emitted " + this.options.statsFile);
        mkdirp(path.dirname(this.options.statsFile));
        fs.writeFile(this.options.statsFile, JSON.stringify(customStats, null, 1), done);
      } else {
        // eslint-disable-next-line no-console
        logger.info("Serving the 'stats.json' file dynamically.");

        this.options.setResults(customStats);
        done();
      }
    });
  }
}

module.exports = StatsPlugin;

/**
 * Collect chunk data for accepted chunk ids.
 * @param statsJson full stats.json content
 * @param acceptedKeys chunk ids that are accepted
 * @returns slimmed down chunks
 */
function collectChunks(statsJson, acceptedChunks) {
  const chunks = [];
  // only handle chunks if they exist for stats
  if (statsJson.chunks) {
    statsJson.chunks.forEach(function (chunk) {
      // Acc chunk if chunk id is in accepted chunks
      if (acceptedChunks.includes(chunk.id)) {
        const modules = [];
        // Add all modules for chunk as slimmed down modules
        chunk.modules.forEach(function (module) {
          const slimModule = {
            id: module.id,
            name: module.name,
            source: module.source
          };
          if (module.modules) {
            slimModule.modules = collectSubModules(module);
          }
          modules.push(slimModule);
        });
        const slimChunk = {
          id: chunk.id,
          names: chunk.names,
          files: chunk.files,
          hash: chunk.hash,
          modules: modules
        }
        chunks.push(slimChunk);
      }
    });
  }
  return chunks;
}

/**
 * Collect all modules that are for a chunk in  acceptedChunks.
 * @param statsJson full stats.json
 * @param acceptedChunks chunk names that are accepted for modules
 * @returns slimmed down modules
 */
function collectModules(statsJson, acceptedChunks) {
  let modules = [];
  // skip if no modules defined
  if (statsJson.modules) {
    statsJson.modules.forEach(function (module) {
      // Add module if module chunks contain an accepted chunk and the module is generated-flow-imports.js module
      if (module.chunks.filter(key => acceptedChunks.includes(key)).length > 0 &&
        (module.name.includes("generated-flow-imports.js") || module.name.includes("generated-flow-imports-fallback.js"))) {
        const slimModule = {
          id: module.id,
          name: module.name,
          source: module.source
        };
        if (module.modules) {
          slimModule.modules = collectSubModules(module);
        }
        modules.push(slimModule);
      }
    });
  }
  return modules;
}

/**
 * Collect any modules under a module (aka. submodules);
 *
 * @param module module to get submodules for
 */
function collectSubModules(module) {
  let modules = [];
  module.modules.forEach(function (submodule) {
    if (submodule.source) {
      const slimModule = {
        name: submodule.name,
        source: submodule.source,
      };
      if (submodule.id) {
        slimModule.id = submodule.id;
      }
      modules.push(slimModule);
    }
  });
  return modules;
}
