var _ = require('lodash');
var ejs = require('ejs');
var utils = require('ejs/lib/utils');

// ES6 class syntax can not be used here. It makes the methods not enumerable,
// that prevents EJS from copying them when using template includes.

function TemplateContext() {
}

TemplateContext.prototype = {
  // Enable usage of the Lodash API in templates
  '_': _,

  getJavaName: function(name) {
    return _.upperFirst(_.camelCase(this.stripNamespace(name)));
  },

  getJavaEventName: function(name) {
    return this.getJavaName(name) + 'Event';
  },

  getJavaType: function(type) {
    return {
      'Boolean': 'boolean',
      'Number': 'double',
      'Date': 'Date',
      'String': 'String',
      'Array': 'JsonArray',
      'Object': 'JsonObject',
      'Element': 'Element',
      'HTMLElement': 'Element'
    }[type] || 'JsonValue';
  },

  getJavaValueForType: function(value, type) {
    switch (type) {
      case 'Boolean':
      case 'Number':
        return value.toString();
      case 'Date':
        return 'new Date("' + value + '")';
      case 'String':
        return '"' + value + '"';
      case 'Array':
      case 'Object':
      default:
        return '((' + this.getJavaType(type) + ') Json.parse("' + value + '"))';
    }
  },

  isJsonJavaType: function(javaType) {
    var jsonTypes = {
      'JsonArray': true,
      'JsonObject': true,
      'JsonValue': true
    };
    return jsonTypes[javaType] || false;
  },

  isTypeSupportedInProperties: function(type) {
    switch (type) {
      case 'Boolean':
      case 'Number':
      case 'String':
      case 'Array':
      case 'Object':
        return true;
      default:
        return false;
    }
  },

  isTypeSupportedInMethodCalls: function(type) {
    switch (type) {
      case 'Boolean':
      case 'Number':
      case 'String':
      case 'Element':
      case 'HTMLElement':
        return true;
      default:
        return false;
    }
  },

  indentLines: function(text, indent) {
    return _.trim(text).split('\n').join('\n' + indent);
  },

  stripNamespace: function(name) {
    return name.split('.').pop();
  },

  getCollectionName: function(name) {
    return this.stripNamespace(name).split(/\W+|(?=[A-Z][a-z0-9]+)/)[0].toLowerCase();
  },

  /**
   * Captures template block with the given block name or returns the captured
   * block content if no block is given.
   *
   * @param {String} blockName The name of the block
   * @param {String=} callback The block callback to capture
   * @return {String} The captured block content
   */
  contentFor: function(blockName, content) {
    var blockNameKey = '__block__' + blockName;
    if (!this[blockNameKey]) this[blockNameKey] = [];
    if (content) {
      this[blockNameKey].push(content);
    }
    return this[blockNameKey].join('');
  }
};


module.exports = TemplateContext;
