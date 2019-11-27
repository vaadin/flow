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
'use strict';

const {Transform} = require('readable-stream');
const path = require('path');
const jsonFile = require('fs-extra');

/**
 * Version reader gulp transform.
 * <p>
 * Doesn't modify the given files in any way, instead just caches the paths to their package.json or bower.json folder.
 * The versions for a specific element can be asked with the VersionReader#getElementVersion(elementName) method.
 * @type {VersionReader}
 */
module.exports = class VersionReader extends Transform {
  constructor(options) {
    if (!options) options = {};
    options.objectMode = true;
    super(options);
    this._jsonFileToVersionMap = new Map();
    this._elementNameToJsonFileMap = new Map();
  }

  /**
   * Caches the location of the package.json or bower.json files for the element in the given path.
   *
   * @param filePath the path to the element sources
   * @private
   */
  _cacheElementJsonPath(filePath) {
    const parsedPath = path.parse(filePath);
    let jsonFilePath = path.join(parsedPath.dir, "package.json");
    if (!jsonFile.existsSync(jsonFilePath)) {
      jsonFilePath = path.join(parsedPath.dir, "bower.json");
    }
    this._elementNameToJsonFileMap.set(parsedPath.name, jsonFilePath);
  }

  /**
   * Gets the version of the given element.
   * <p>
   * The version is read from the package.json or bower.json for the given element.
   * Since some elements are so tightly coupled that they are in the same folder,
   * they also share the same version. Thus the versions are cached to avoid unnecessary
   * reading of the json files.
   *
   * @param elementName the name of the element, e.g. "paper-input"
   * @returns String the version string, e.g. "2.0.1"
   */
  getElementVersion(elementName) {
    const jsonPath = this._elementNameToJsonFileMap.get(elementName);
    if (typeof jsonPath === 'undefined') {
      console.error(`Cannot resolve package.json or bower.json file path for element: ${elementName}`);
      return 'UNKNOWN';
    }

    if (!this._jsonFileToVersionMap.has(jsonPath)) {
      const json = jsonFile.readJSONSync(jsonPath);
      this._jsonFileToVersionMap.set(jsonPath, json.version);
    }

    return this._jsonFileToVersionMap.get(jsonPath);
  }

  _transform(file, encoding, callback) {
    this._cacheElementJsonPath(file.path);
    this.push(file);
    callback();
  }
};
