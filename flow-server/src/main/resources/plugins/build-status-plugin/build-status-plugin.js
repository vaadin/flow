/*
 * Copyright 2000-2022 Vaadin Ltd.
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

/**
 * This plugin handles communicates to Java dev-mode handler status of compilation
 */
class BuildStatusPlugin {
  constructor(options = {}) {
    this.options = options;
  }

  apply(compiler) {
    const logger = compiler.getInfrastructureLogger('build-status');

    compiler.hooks.done.tap('done', (stats) => {
      // Defer notification and let the webpack-dev-server finish
      setTimeout(() => {
        const errors = stats.compilation.errors;
        const warnings = stats.compilation.warnings;
        if (errors.length > 0) {
          logger.info(
            `${humanReadable(errors.length, 'error')} and ${humanReadable(warnings.length, 'warning')} were reported.`
          );
          logger.info(': Failed to compile.');
        } else {
          if (warnings.length > 0) {
            logger.info(`${humanReadable(warnings.length, 'warning')} were reported.`);
          }
          logger.info(': Compiled.');
        }
      }, 0);
    });
  }
}

function humanReadable(count, label) {
  if (count % 10 == 1) {
    return `${count} ${label}`;
  } else {
    return `${count} ${label}s`;
  }
}

module.exports = BuildStatusPlugin;
