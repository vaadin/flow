/*
 * Copyright 2000-2017 Vaadin Ltd.
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
'use strict';

const {Transform} = require('readable-stream');
const path = require('path');
const jsonFile = require('fs-extra')

/**
 * Version reader gulp transform.
 * <p>
 * Doesn't modify the given files in any way, instead just caches the paths to their bower.json folder.
 * The versions for a specific element can be asked with the VersionReader#getElementVersion(elementName) method.
 * @type {VersionReader}
 */
module.exports = class VersionReader extends Transform {
  constructor(options) {
    if (!options) options = {};
    options.objectMode = true;
    super(options);
    this._bowerJsonFileToVersionMap = new Map();
    this._elementNameToBowerJsonFileMap = new Map();
  }

  /**
   * Caches the location of the bower.json file for the element in the given path.
   *
   * @param filePath the path to the element sources
   * @private
   */
  _cacheElementBowerJsonPath(filePath) {
    const parsedPath = path.parse(filePath);
    const bowerJsonPath = path.join(parsedPath.dir, "bower.json");
    this._elementNameToBowerJsonFileMap.set(parsedPath.name, bowerJsonPath);
  }

  /**
   * Gets the version of the given element.
   * <p>
   * The version is read from the bower.json for the given element.
   * Since some elements are so tightly coupled that they are in the same folder,
   * they also share the same version. Thus the versions are cached to avoid unnecessary
   * reading of the bower.json files.
   *
   * @param elementName the name of the element, e.g. "paper-input"
   * @returns String the version string, e.g. "2.0.1"
   */
  getElementVersion(elementName) {
    const bowerJsonPath = this._elementNameToBowerJsonFileMap.get(elementName);
    if (typeof bowerJsonPath === 'undefined') {
      console.error(`Cannot resolve bower.json file path for element: ${elementName}`);
      return 'UNKNOWN';
    }

    if (!this._bowerJsonFileToVersionMap.has(bowerJsonPath)) {
      const json = jsonFile.readJSONSync(bowerJsonPath);
      this._bowerJsonFileToVersionMap.set(bowerJsonPath, json.version);
    }

    return this._bowerJsonFileToVersionMap.get(bowerJsonPath);
  }

  _transform(file, encoding, callback) {
    this._cacheElementBowerJsonPath(file.path);
    this.push(file);
    callback();
  }
};