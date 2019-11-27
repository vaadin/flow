/*
 * Copyright 2000-2019 Vaadin Ltd.
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
  * Class for resolving mixin and behavior hierarchies.
  *
  * @type {MixinCollector}
  */
module.exports = class MixinCollector {

  constructor() {
    this._mixinMap = {};
    this._behaviorMap = {};
  }

  /**
   * Get the full flattened hierarchy of mixins for the given
   * element identifier.
   *
   * @param elementIdentifier
   * @returns {Array}
   */
  getFlattenedMixinHierarchy(elementIdentifier) {
    this._loopDetect = new Set();
    try {
      return this._internalGetFlattenedHierarchy(this._mixinMap, elementIdentifier);
    } catch(e) {
      console.error(`Loop detected in mixin hierarchy for element ${elementIdentifier}. `
      + `Please verify the mixin definitions for descendants of element ${e.message}.`);
      return []
    }
  }

  /**
   * Store information in this instance by mapping
   * an element to a list of its mixins.
   *
   * @param elementIdentifier
   * @param mixins
   */
  putMixins(elementIdentifier, mixins) {
    this._mixinMap[elementIdentifier] = mixins;
  }

  /**
   * Get the full flattened hierarchy of behaviors for the given
   * element identifier.
   *
   * @param elementIdentifier
   * @returns {Array}
   */
  getFlattenedBehaviorHierarchy(elementIdentifier) {
    this._loopDetect = new Set();
    try {
      return this._internalGetFlattenedHierarchy(this._behaviorMap, elementIdentifier);
    } catch(e) {
      console.error(`Loop detected in behavior hierarchy for element ${elementIdentifier}. `
      + `Please verify the behavior definitions for descendants of element ${e.message}.`);
      return []
    }
  }

  /**
   * Store information in this instance by mapping
   * an element to a list of its behaviors.
   *
   * @param elementIdentifier
   * @param behaviors
   */
  putBehaviors(elementIdentifier, behaviors) {
    this._behaviorMap[elementIdentifier] = behaviors;
  }

  _internalGetFlattenedHierarchy(mixinMap, elementIdentifier) {
    if (this._loopDetect.has(elementIdentifier)) {
      throw new Error(elementIdentifier);
    }
    this._loopDetect.add(elementIdentifier);
    const mixins = mixinMap[elementIdentifier];
    if (!mixins) {
      return [];
    }
    const allMixins = mixins
      .map(childMixin => this._internalGetFlattenedHierarchy(mixinMap, childMixin))
      .reduce((union, childMixins) => {
        return new Set([...union, ...childMixins]);
      }, new Set(mixins));
    return [...allMixins];
  }
};
