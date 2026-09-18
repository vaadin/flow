"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const logging = require("plylog");
const polymer_project_config_1 = require("polymer-project-config");
const vinyl_fs_1 = require("vinyl-fs");
const analyzer_1 = require("./analyzer");
const base_tag_updater_1 = require("./base-tag-updater");
const bundle_1 = require("./bundle");
const custom_elements_es5_adapter_1 = require("./custom-elements-es5-adapter");
const inject_babel_helpers_1 = require("./inject-babel-helpers");
const prefetch_links_1 = require("./prefetch-links");
const push_manifest_1 = require("./push-manifest");
const logger = logging.getLogger('polymer-project');
class PolymerProject {
    constructor(config) {
        if (config.constructor.name === 'ProjectConfig') {
            this.config = config;
        }
        else if (typeof config === 'string') {
            const maybeConfig = polymer_project_config_1.ProjectConfig.loadConfigFromFile(config);
            if (maybeConfig == null) {
                throw new Error(`Unable to load config from file: ${config}`);
            }
            this.config = maybeConfig;
        }
        else {
            this.config = new polymer_project_config_1.ProjectConfig(config);
        }
        logger.debug(`build config loaded:`, this.config);
        this.analyzer = new analyzer_1.BuildAnalyzer(this.config);
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
    bundler(options) {
        return new bundle_1.BuildBundler(this.config, this.analyzer, options);
    }
    /**
     * Returns the analyzer's stream of this project's source files - files
     * matched by the project's `config.sources` value.
     */
    sources() {
        return this.analyzer.sources();
    }
    /**
     * Returns the analyzer's stream of this project's dependency files - files
     * loaded inside the analyzed project that are not considered source files.
     */
    dependencies() {
        let dependenciesStream = this.analyzer.dependencies();
        // If we need to include additional dependencies, create a new vinyl
        // source stream and pipe our default dependencyStream through it to
        // combine.
        if (this.config.extraDependencies.length > 0) {
            const includeStream = vinyl_fs_1.src(this.config.extraDependencies, {
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
    addPrefetchLinks() {
        return new prefetch_links_1.AddPrefetchLinks(this.config);
    }
    /**
     * Returns a stream transformer that adds a push manifest file to the set
     * of all input files that pass through.
     */
    addPushManifest(outPath, basePath) {
        return new push_manifest_1.AddPushManifest(this.config, outPath, basePath);
    }
    /**
     * Returns a stream transformer that injects `custom-elements-es5-adapter.js`
     * into the entry point HTML file. This adapter is needed when serving ES5
     * to browsers that support the native Custom Elements API.
     */
    addCustomElementsEs5Adapter() {
        return new custom_elements_es5_adapter_1.CustomElementsEs5AdapterInjector();
    }
    addBabelHelpersInEntrypoint(entrypoint = this.config.entrypoint) {
        return new inject_babel_helpers_1.BabelHelpersInjector(entrypoint);
    }
    /**
     * Return a stream transformer that updates the `<base>` tag of the project's
     * entrypoint HTML file with the given new value. No change is made if a
     * `<base>` tag does not already exist.
     */
    updateBaseTag(baseHref) {
        return new base_tag_updater_1.BaseTagUpdater(this.config.entrypoint, baseHref);
    }
}
exports.PolymerProject = PolymerProject;
//# sourceMappingURL=polymer-project.js.map