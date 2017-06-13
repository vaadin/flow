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
const globalVar = require('./global-variables');
const path = require('path');
const File = require('vinyl');

const skipInherited = (inheritedFrom) => {
  return globalVar.skipInheritedAPI && inheritedFrom === 'undefined';
};

const getType = (type) => {
  if (typeof type === 'undefined') {
    type = "OBJECT";
  } else {
    type = type.toUpperCase();
  }

  if (type === 'STRING' ||
    type === 'BOOLEAN' ||
    type === 'NUMBER' ||
    type === 'OBJECT' ||
    type === 'ARRAY') {
    return type;
  } else {
    return 'OBJECT';
  }
};

/**
 * Converts properties Map to desired JSON output:
 *
 * "properties": [
 * {"name": "stringProperty", "type": "STRING", "description": "Description of the property"},
 * {"name": "numberProperty", "type": "NUMBER"},
 * {"name": "booleanProperty", "type": "BOOLEAN"},
 * {"name": "arrayProperty", "type": "ARRAY"},
 * {"name": "objectProperty", "type": "OBJECT"},
 * {"name": "stringReadOnlyProperty", "type": "STRING", "readOnly": true}
 * ]
 *
 * Inherited properties are skipped.
 *
 * @param properties a Map of the PolymerElement.properties
 * @returns {Array} of properties json data
 */
const propertiesToJsonArray = (properties) => {
  const propertiesJson = [];
  for (let property of properties.values()) {
    if (!skipInherited(property.inheritedFrom) && property.privacy === 'public') {
      const propertyJson = {
        "name": property.name,
        "type": getType(property.type),
        "description": property.jsdoc ? property.jsdoc.description : 'Missing documentation!',
// TODO #1768 "readonly": true
      }
      propertiesJson.push(propertyJson);
    }
  }
  console.log(`    Wrote ${propertiesJson.length} properties out of ${properties.size}`);
  return propertiesJson;
};

/**
 * Converts method parameters array to desired JSON output:
 *
 * "parameters" : [
 * {"name" : "paramName", "type": "STRING" },
 * {"name" : "optionalParameter", "type": "NUMBER", "optional" : true }
 * ]}
 *
 * @param parameters the parameters array from method.parameters
 * @returns {Array} of parameter json data
 */
const parametersToJsonArray = (parameters) => {
  const parametersJson = [];
  for (let parameter of parameters) {
    const parameterJson = {
      "name": parameter.name,
// TODO #1771 write multiple accepted types as an array
      "type": getType(parameter.type),
      "description": parameter.description ? parameter.description :
        (parameter.desc ? parameter.desc : "Missing documentation!"),
// TODO #1767 "optional": false
    };
    parametersJson.push(parameterJson);
  }
  return parametersJson;
};

/**
 * Converts methods Map to desired json output:
 *
 * "functions": [
 *  {"name": "doSomething",
 *   "returns" : "STRING",
 *   "description": "Call this method to do something",
 *   "parameters" : [ ... ]
 *  },
 *  {"name": "doSomethingElse",
 *   "parameters" : [ ... ]
 * }]
 *
 * Inherited and non-public methods are skipped.
 *
 * @param methods a Map of the PolymerElement.methods
 * @returns {Array} of methods json data
 */
const methodsToJsonArray = (methods) => {
  const methodsJson = [];
  for (let method of methods.values()) {
    // do not add inherited or non-public functions
    if (!skipInherited(method.inheritedFrom) && method.privacy === 'public') {
      const methodJson = {
        "name": method.name,
        "description": method.jsdoc ? method.jsdoc.description : "Missing documentation!",
        "parameters": parametersToJsonArray(method.params)
      };
      methodsJson.push(methodJson);
    }
  }
  console.log(`    Wrote ${methodsJson.length} methods out of ${methods.size}`);
  return methodsJson;
};

/**
 * Converts events Map to desired json output:
 *
 * "events" : [
 *  {"name" : "close", "description": "This event is called when the X button is clicked",
 *  "properties" : [
 *    {"name" : "property1", "type": "STRING" },
 *    {"name" : "property2", "type": "NUMBER" }
 * ]}]
 *
 * @param events a Map of the PolymerElement.events
 * @returns {Array} of events json data
 */
const eventsToJsonArray = (events) => {
  const eventsJson = [];
  for (let event of events.values()) {
    if (!skipInherited(event.inheritedFrom)) {
      const eventJson = {
        "name": event.name,
        "description": event.jsondoc ? event.jsondoc.description :
          (event.description ? event.description : "Missing documentation!"),
        "properties": parametersToJsonArray(event.params)
      };
      eventsJson.push(eventJson);
    }
  }
  console.log(`    Wrote ${eventsJson.length} methods out of ${events.size}`);
  return eventsJson;
};

module.exports = class ElementJsonTransform extends Transform {
  constructor(versionReader) {
    const options = {};
    options.objectMode = true;
    super(options);
    this.versionReader = versionReader;
  }

  /**
   * Transform analyzer data to desired JSON output:
   *
   * {
   * "name": "MyComponent",
   * "tag": "my-component",
   * "baseUrl": "my-component/my-component.html",
   * "version": "v.1.0.0",
   * "properties": [ ... ],
   * "functions": [ ... ],
   * "events" : [ ... ],
   * "behaviors": ["focusable", "clickable", "resizable"]
   * "description": "This is my component",
   * }
   *
   * @param element the PolymerElement from Analyzer
   * @param encoding encoding (utf8)
   * @param callback the callback triggered
   * @private
   */
  _transform(element, encoding, callback) {
    console.info("Generating JSON for " + element.tagName);
    const version = this.versionReader.getElementVersion(element.tagName);
    const json = {
      "name": element.name ? element.name : element.tagName,
      "tag": element.tagName,
      "baseUrl": element._parsedDocument.baseUrl,
      "version": version,
      "properties": propertiesToJsonArray(element.properties),
      "methods": methodsToJsonArray(element.methods),
      "events": eventsToJsonArray(element.events),
      "listeners": element.listers,
      "behaviors": element.behaviorAssignments.map(behavior => behavior.name),
      "description": element.jsdoc ? element.jsdoc.description : "Missing documentation!"
    };

    const file = new File({
      path: path.join(globalVar.targetDir, element.tagName + '.json'),
      contents: new Buffer(JSON.stringify(json, null, 2))
    });
    callback(null, file);
  }
}
