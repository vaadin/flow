"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
/**
 * This is a special map which automatically creates
 * a default value if the key is not found.
 */
class MapWithDefault extends Map {
    constructor(makeDefault) {
        super();
        this.makeDefault = makeDefault;
    }
    get(key) {
        if (!this.has(key)) {
            super.set(key, this.makeDefault(key));
        }
        return super.get(key);
    }
}
exports.MapWithDefault = MapWithDefault;
//# sourceMappingURL=map.js.map