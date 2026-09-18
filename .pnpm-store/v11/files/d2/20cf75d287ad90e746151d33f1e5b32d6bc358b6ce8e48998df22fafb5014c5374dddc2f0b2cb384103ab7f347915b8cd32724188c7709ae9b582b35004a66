"use strict";
/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
const whatwgUrl = require("whatwg-url");
const nodeResolve = require("resolve");
const path_1 = require("path");
const isWindows = require("is-windows");
const pathIsInside = require('path-is-inside');
const isPathSpecifier = (s) => /^\.{0,2}\//.test(s);
/**
 * Resolves module specifiers using node module resolution.
 *
 * Full URLs - those parsable by the WHATWG URL spec - are returned as-is.
 * Absolute and relative paths are resolved, even though they are valid
 * HTML-spec module specifiers, because node resolution supports directories
 * and omitting extensions. If a specifier doesn't resolve, it's returned as-is.
 *
 * @param componentInfo An object describing a "component-style" URL layout. In
 *   this layout, cross-package URLs reach out of the package directory to
 *   sibling packages, rather than into the component directory. When given,
 *   this parameter causes relative paths to be returns for this style.
 */
exports.resolve = (specifier, documentPath, componentInfo) => {
    if (whatwgUrl.parseURL(specifier) !== null) {
        return specifier;
    }
    const importerFilepath = documentPath;
    const dependencyFilepath = nodeResolve.sync(specifier, {
        basedir: path_1.dirname(importerFilepath),
        moduleDirectory: ['bower_components', 'node_modules'],
        // It's invalid to load a .json or .node file as a module on the web,
        // but this is what Node's resolution algorithm does
        // (https://nodejs.org/api/modules.html#modules_all_together), so we
        // also do it here for completeness. Without including these
        // extensions the user will probably get a 404. With them, they'll
        // probably get an invalid MIME type error (which is hopefully more
        // useful).
        extensions: ['.js', '.json', '.node'],
        // Some packages use a non-standard alternative to the "main" field
        // in their package.json to differentiate their ES module version.
        packageFilter: (packageJson) => {
            packageJson.main = packageJson.module ||
                packageJson['jsnext:main'] || packageJson.main;
            return packageJson;
        },
    });
    let relativeSpecifierUrl = path_1.relative(path_1.dirname(importerFilepath), dependencyFilepath);
    if (componentInfo !== undefined) {
        // Special handling for servers like Polyserve which, when serving a
        // package "foo", will map the URL "/components/foo" to the root package
        // directory, so that "foo" can make correct relative path references to
        // its dependencies.
        //
        // Note that Polyserve will only set componentInfo if the particular
        // request was for a URL path in the components/ directory.
        const { packageName, rootDir, componentDir } = componentInfo;
        const importerInRootPackage = !pathIsInside(importerFilepath, componentDir);
        const dependencyInRootPackage = !pathIsInside(dependencyFilepath, componentDir);
        if (importerInRootPackage && !dependencyInRootPackage) {
            // A module from the root package, served from a components/ URL, is
            // importing a module from a different package. In this case we need
            // to fix up our relative path specifier, because on disk the
            // dependency resolves to e.g. "./node_modules/foo", but in URL space
            // it must resolve to "../foo".
            //
            // Note that the case where both the importer and the dependency are
            // in the root package does not need to be fixed up, since the
            // relative path works out the same.
            const rootRelativeImporterPath = path_1.relative(rootDir, importerFilepath);
            const effectiveImporterFilepath = path_1.join(componentDir, packageName, rootRelativeImporterPath);
            relativeSpecifierUrl = path_1.relative(path_1.dirname(effectiveImporterFilepath), dependencyFilepath);
        }
    }
    if (isWindows()) {
        // normalize path separators to URL format
        relativeSpecifierUrl =
            relativeSpecifierUrl.replace(/\\/g, '/');
    }
    if (!isPathSpecifier(relativeSpecifierUrl)) {
        relativeSpecifierUrl = './' + relativeSpecifierUrl;
    }
    return relativeSpecifierUrl;
};
//# sourceMappingURL=resolve-specifier-node.js.map