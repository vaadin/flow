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
'use strict';

const globalVar = require('./global-variables');
const jsonFile = require('fs-extra');
const path = require('path');

/**
 * Filter for files to be used for the analysis. It reads and parses the
 * dependencies file (like bower.json), and use the explicitly declared
 * dependencies as the base folder for allowed webcomponents.
 * <p>
 * For example: when declaring "vaadin-button" as dependency, only webcomponents
 * inside the "**\/vaadin-button/" folder are accepted.
 * @type {ElementFilter}
 */
module.exports = class ElementFilter {
  constructor() {
    this._dependencies = [];
    this._readDependenciesFile(globalVar.dependenciesFile);
  }

/**
 * Reads and parses the dependecy file.
 *
 * @param filePath the path to the dependecy file
 * @private
 */
  _readDependenciesFile(filePath) {
    const json = jsonFile.readJSONSync(filePath);
    this._dependencies = Object.keys(json.dependencies);
  }

/**
 * Evaluates whether a given file should be accepted for the analysis. It uses
 * 'file.path' to extract the complete path of the file.
 *
 * @param file the file to be evaluated
 * @returns true if the file should be analyzed, false otherwise
 */
  acceptFile(file) {
    return this.acceptPath(file.path);
  }


  /**
   * Evaluates whether a given file should be accepted for the analysis.
   *
   * @param filePath the string with the path of the file to be evaluated
   * @returns true if the file should be analyzed, false otherwise
   */
  acceptPath(filePath) {
    const folders = path.dirname(filePath.replace('/', path.sep)).split(path.sep);
    // we'll only analyze files the root or inside /src
    let folderName = folders.pop();
    if (folderName.indexOf('src') === 0) {
      folderName = folders.pop();
    }
    return this._dependencies.indexOf(folderName) >= 0;
  }

};
