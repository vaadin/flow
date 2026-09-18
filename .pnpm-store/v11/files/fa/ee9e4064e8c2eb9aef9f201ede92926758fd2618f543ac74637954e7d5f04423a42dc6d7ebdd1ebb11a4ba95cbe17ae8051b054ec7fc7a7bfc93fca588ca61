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
const browser_capabilities_1 = require("browser-capabilities");
const content_type_1 = require("content-type");
const polymer_build_1 = require("polymer-build");
const get_compile_target_1 = require("./get-compile-target");
const transform_middleware_1 = require("./transform-middleware");
/**
 * Returns an express middleware that injects the Custom Elements ES5 Adapter
 * into the entry point when we are serving ES5.
 *
 * This is a *transforming* middleware, so it must be installed before the
 * middleware that actually serves the entry point.
 */
function injectCustomElementsEs5Adapter(compile) {
    return transform_middleware_1.transformResponse({
        shouldTransform(request, response) {
            const capabilities = browser_capabilities_1.browserCapabilities(request.get('user-agent'));
            const compileTarget = get_compile_target_1.getCompileTarget(capabilities, compile);
            const contentTypeHeader = response.get('Content-Type');
            const contentType = contentTypeHeader && content_type_1.parse(contentTypeHeader).type;
            // We only need to inject the adapter if we are compiling to ES5.
            return contentType === 'text/html' && compileTarget === 'es5';
        },
        transform(_request, _response, body) {
            // TODO(aomarks) This function will make no changes if the body does
            // not find a web components polyfill script tag. This is the heuristic
            // we use to determine if a file is the entry point. We would instead
            // be able to check explicitly for the entry point in `shouldTransform`
            // if we had the project config available.
            return polymer_build_1.addCustomElementsEs5Adapter(body);
        },
    });
}
exports.injectCustomElementsEs5Adapter = injectCustomElementsEs5Adapter;
//# sourceMappingURL=custom-elements-es5-adapter-middleware.js.map