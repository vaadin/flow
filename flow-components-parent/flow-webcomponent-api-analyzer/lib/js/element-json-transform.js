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
const globalVar = require('./global-variables');
const path = require('path');
const File = require('vinyl');

const getObjectType = (type) => {
  // This is for JS Object types that have some properties
  // {inputElement: (Element|undefined), value: (string|undefined), invalid: boolean}
  const objectTypesJson = [];
  if (typeof type !== 'undefined' && type.startsWith('{') && type.endsWith('}')) {
    // remove { and } and split from commas
    const types = type.substring(1, type.length - 1).split(',');
    for (let option of types) {
      option = option.trim();
      const name = option.substring(0, option.indexOf(':'));
      const objectType = option.substring(option.indexOf(':') + 1, option.length).trim();
      const objectTypeJson = {
        "name": name,
        "type": getTypes(objectType)
      };
      if (objectType.includes('undefined')) {
        objectTypeJson['optional'] = true;
      }
      objectTypesJson.push(objectTypeJson);
    }
  }
  if (objectTypesJson.length > 0) {
    return [{"innerTypes": objectTypesJson}];
  }
  return [];
};

const getTypes = (type) => {
  const types = [];
  if (typeof type === 'undefined') {
    types.push(getType(type));
  } else if (type.startsWith('{') && type.endsWith('}')) {
    // handled separately via getObjectType(type)
  } else if (type.includes('|')) {
    // might be wrapped with parenthesis
    if (type.startsWith('(') && type.endsWith(')')) {
      type = type.substring(1, type.length - 1);
    }
    for (let someType of type.split('|')) {
      if (someType.trim() === 'undefined') {
        // this means it is optional, handled elsewhere
        continue;
      }
      types.push(getType(someType));
    }
  } else {
    types.push(getType(type));
  }
  return types;
};

const getType = (type) => {
  if (typeof type === 'undefined') {
    console.warn(`Undefined type, missing jsdoc parameter ${type}`);
    type = "UNDEFINED";
  } else {
    type = type.toUpperCase();
  }

  if (type === 'STRING' ||
    type === 'BOOLEAN' ||
    type === 'NUMBER' ||
    type === 'OBJECT' ||
    type === 'ARRAY' ||
    type === 'FUNCTION') {
    return type;
  } else {
    console.warn(`Unsupported type requested: ${type}`);
    return 'OBJECT';
  }
};

const isOptional = (type) => {
  if (typeof type !== 'string') {
    console.warn(`isOptional called with non-string parameter ${type}`);
    return false;
  }
  const possibleTypes = type.match(/[A-Za-z]+/g);
  return possibleTypes ? possibleTypes.includes('undefined') : false;
};

module.exports = class ElementJsonTransform extends Transform {
  constructor(versionReader, mixinCollector, variantsData) {
    const options = {};
    options.objectMode = true;
    super(options);
    this.versionReader = versionReader;
    this.mixinCollector = mixinCollector;
    this.variantsData = variantsData;
    this.elementClassToProcessedResult = new Map();
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
   * "behaviors": ["focusable", "clickable", "resizable"],
   * "variants": { themeName : [ ... ], ... },
   * "description": "This is my component",
   * }
   *
   * @param element the PolymerElement from Analyzer
   * @param encoding encoding (utf8)
   * @param callback the callback triggered
   * @private
   */
  _transform(element, encoding, callback) {
    const elementTagName = element.tagName;
    console.info("Generating JSON for " + elementTagName);
    const elementClassName = element.name;

    const resultingName = elementClassName ? elementClassName : elementTagName;
    const version = this.versionReader.getElementVersion(elementTagName);
    const parentClassData = element.superClass
        ? this.elementClassToProcessedResult.get(element.superClass.identifier)
        : null;

    const json = {
      "name": resultingName,
      "tag": elementTagName,
      "baseUrl": element._parsedDocument.baseUrl,
      "version": version,
      "properties": this.propertiesToJsonArray(element.properties, parentClassData),
      "methods": this.methodsToJsonArray(element.methods, parentClassData),
      "events": this.eventsToJsonArray(element.events, parentClassData),
      "slots": this.slotsToJsonArray(element.slots),
      "behaviors": this.mixinCollector.getFlattenedBehaviorHierarchy(resultingName),
      "mixins": this.mixinCollector.getFlattenedMixinHierarchy(resultingName),
      "variants": this.variantsData[elementTagName] || {},
      "description": element.jsdoc ? element.jsdoc.description : "Missing documentation!"
    };

    if (parentClassData) {
      json.parentTagName = parentClassData.tag;
    }

    if (elementClassName) {
        this.elementClassToProcessedResult.set(elementClassName, json)
    }

    const file = new File({
      path: path.join(globalVar.targetDir, elementTagName + '.json'),
      contents: new Buffer(JSON.stringify(json, null, 2) + '\n')
    });

    callback(null, file);
  }

  skipInherited(mixinInheritedFrom, parentClassData) {
    return globalVar.skipInheritedAPI && parentClassData && (mixinInheritedFrom === parentClassData.name ||
      (parentClassData.mixins && parentClassData.mixins.includes(mixinInheritedFrom)));
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
   * {"name": "propertyWithNotify", "type": "STRING", "notify": true}
   * ]
   *
   * Inherited properties are skipped.
   *
   * @param properties a Map of the PolymerElement.properties
   * @param parentClassData data on the closest superclass or null, if none present
   * @returns {Array} of properties json data
   */
  propertiesToJsonArray(properties, parentClassData) {
    const propertiesJson = [];
    for (let property of properties.values()) {
      if (typeof property !== 'undefined' && !this.skipInherited(property.inheritedFrom, parentClassData) && property.privacy === 'public') {
        const propertyJson = {
          "name": property.name,
          // property can be of only one type, but in case there is an object property, need to use an empty array here
          "type": getTypes(property.type),
          "objectType": getObjectType(property.type),
          "description": property.jsdoc ? property.jsdoc.description : 'Missing documentation!',
        };
        if (property.readOnly) {
          propertyJson.readOnly = true;
        }
        if (property.notify) {
          propertyJson.notify = true;
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
   * {"name" : "paramName", "type": {"STRING", "ELEMENT} },
   * {"name" : "optionalParameter", "types": {"NUMBER"}, "optional" : true }
   * ]}
   *
   * @param parameters the parameters array from method.parameters
   * @returns {Array} of parameter json data
   */
  parametersToJsonArray(parameters) {
    const parametersJson = [];
    if (typeof parameters === 'undefined') {
      return parametersJson;
    }
    for (let parameter of parameters) {
      // ignore parameters of the type 'Event' - they don't have meaningful data
      if (parameter.type === 'Event') {
        continue;
      }
      const parameterJson = {
        "name": parameter.name,
        "type": getTypes(parameter.type),
        "objectType": getObjectType(parameter.type),
        "description": parameter.description ? parameter.description :
          (parameter.desc ? parameter.desc : "Missing documentation!")
      };
      if (isOptional(parameter.type)) {
        parameterJson.optional = true;
      }
      parametersJson.push(parameterJson);
    }
    return parametersJson;
  };

  /**
   * Converts methods Map to desired json output:
   *
   * "methods": [
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
   * @param parentClassData data on the closest superclass or null, if none present
   * @returns {Array} of methods json data
   */
  methodsToJsonArray(methods, parentClassData) {
    const methodsJson = [];
    for (let method of methods.values()) {
      // do not add inherited or non-public functions
      if (typeof method !== 'undefined' && !this.skipInherited(method.inheritedFrom, parentClassData) && method.privacy === 'public') {
        const methodJson = {
          "name": method.name,
          "description": method.jsdoc ? method.jsdoc.description : "Missing documentation!",
          "parameters": this.parametersToJsonArray(method.params),
          "returns": typeof method['return'] === 'undefined' ? 'UNDEFINED' : getType(method['return'].type)
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
   * @param parentClassData data on the closest superclass or null, if none present
   * @returns {Array} of events json data
   */
  eventsToJsonArray(events, parentClassData) {
    const eventsJson = [];
    for (let event of events.values()) {
      if (typeof event !== 'undefined' && !this.skipInherited(event.inheritedFrom, parentClassData)) {
        const eventJson = {
          "name": event.name,
          "description": event.jsondoc ? event.jsondoc.description :
            (event.description ? event.description : "Missing documentation!"),
          "properties": this.parametersToJsonArray(event.params)
        };
        eventsJson.push(eventJson);
      }
    }
    console.log(`    Wrote ${eventsJson.length} events out of ${events.size}`);
    return eventsJson;
  };

  /**
   * Converts an array of Slot objects to desired json output:
   *
   * "slots": ["", "named-slot", "another-named-slot"]
   * @param slots
   * @returns {Array}
   */
  slotsToJsonArray(slots) {
    const slotsJson = [];
    for (let slot of slots) {
      const name = slot.name;
      if (typeof name === 'undefined' || name === '') {
        slotsJson.push("");
      } else {
        slotsJson.push(name);
      }
    }
    return slotsJson;
  };
};
