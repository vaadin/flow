/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

import * as logging from 'plylog';
import {PackageRelativeUrl} from 'polymer-analyzer';
import {ProjectConfig, ProjectOptions} from 'polymer-project-config';
import {src as vinylSrc} from 'vinyl-fs';

import {BuildAnalyzer} from './analyzer';
import {BaseTagUpdater} from './base-tag-updater';
import {BuildBundler, Options as BuildBundlerOptions} from './bundle';
import {CustomElementsEs5AdapterInjector} from './custom-elements-es5-adapter';
import {BabelHelpersInjector} from './inject-babel-helpers';
import {LocalFsPath} from './path-transformers';
import {AddPrefetchLinks} from './prefetch-links';
import {AddPushManifest} from './push-manifest';

const logger = logging.getLogger('polymer-project');


export class PolymerProject {
  config: ProjectConfig;

  /**
   * A `Transform` stream that uses polymer-analyzer to analyze the files. It
   * can be used to get information on dependencies and fragments for the
   * project once the source & dependency streams have been piped into it.
   */
  analyzer: BuildAnalyzer;

  constructor(config: ProjectConfig|ProjectOptions|string) {
    if (config.constructor.name === 'ProjectConfig') {
      this.config = <ProjectConfig>config;
    } else if (typeof config === 'string') {
      const maybeConfig = ProjectConfig.loadConfigFromFile(config);
      if (maybeConfig == null) {
        throw new Error(`Unable to load config from file: ${config}`);
      }
      this.config = maybeConfig;
    } else {
      this.config = new ProjectConfig(config);
    }

    logger.debug(`build config loaded:`, this.config);

    this.analyzer = new BuildAnalyzer(this.config);
  }

  /**
   * Returns a `Transform` stream that modifies the files that pass through it
   * based on the dependency analysis done by the `analyzer` transform. It
   * "bundles" a project by injecting its dependencies into the application
   * fragments themselves, so that a minimum number of requests need to be made
   * to load.
   *
   * (NOTE: The analyzer stream must be in the pipeline somewhere before this.)
   */
  bundler(options?: BuildBundlerOptions): BuildBundler {
    return new BuildBundler(this.config, this.analyzer, options);
  }

  /**
   * Returns the analyzer's stream of this project's source files - files
   * matched by the project's `config.sources` value.
   */
  sources(): NodeJS.ReadableStream {
    return this.analyzer.sources();
  }

  /**
   * Returns the analyzer's stream of this project's dependency files - files
   * loaded inside the analyzed project that are not considered source files.
   */
  dependencies(): NodeJS.ReadableStream {
    let dependenciesStream: NodeJS.ReadableStream =
        this.analyzer.dependencies();

    // If we need to include additional dependencies, create a new vinyl
    // source stream and pipe our default dependencyStream through it to
    // combine.
    if (this.config.extraDependencies.length > 0) {
      const includeStream = vinylSrc(this.config.extraDependencies, {
        cwdbase: true,
        nodir: true,
        passthrough: true,
      });
      dependenciesStream = dependenciesStream.pipe(includeStream);
    }

    return dependenciesStream;
  }

  /**
   * Returns a stream transformer that injects 'prefetch' link tags into HTML
   * documents based on the transitive dependencies of the document.
   * For entrypoint documents without `<base>` tag, absolute urls are used in
   * prefetch link hrefs.  In all other cases, link hrefs will be relative urls.
   */
  addPrefetchLinks(): NodeJS.ReadWriteStream {
    return new AddPrefetchLinks(this.config);
  }

  /**
   * Returns a stream transformer that adds a push manifest file to the set
   * of all input files that pass through.
   */
  addPushManifest(outPath?: LocalFsPath, basePath?: PackageRelativeUrl):
      NodeJS.ReadWriteStream {
    return new AddPushManifest(this.config, outPath, basePath);
  }

  /**
   * Returns a stream transformer that injects `custom-elements-es5-adapter.js`
   * into the entry point HTML file. This adapter is needed when serving ES5
   * to browsers that support the native Custom Elements API.
   */
  addCustomElementsEs5Adapter(): NodeJS.ReadWriteStream {
    return new CustomElementsEs5AdapterInjector();
  }

  addBabelHelpersInEntrypoint(entrypoint: string = this.config.entrypoint):
      NodeJS.ReadWriteStream {
    return new BabelHelpersInjector(entrypoint);
  }

  /**
   * Return a stream transformer that updates the `<base>` tag of the project's
   * entrypoint HTML file with the given new value. No change is made if a
   * `<base>` tag does not already exist.
   */
  updateBaseTag(baseHref: string): NodeJS.ReadWriteStream {
    return new BaseTagUpdater(this.config.entrypoint as LocalFsPath, baseHref);
  }
}
