'use strict';

var _ = require('lodash');
var fs = require('fs-extra');
var path = require('path');
var TemplateContext = require('./template-context');
var globalVar = require('./global-variables');

module.exports = class Behavior extends TemplateContext {
  constructor(filePath, descriptor) {
    super();

    this.path = filePath;

    // The following keys are copied from the hydrolysis descriptor
    [
      'is',
      'type',
      'behaviors',
      'properties',
      'events'
    ].forEach((prop) => this[prop] = descriptor[prop]);

    this.publicProperties = [];
    this.publicMethods = [];
    this.properties.forEach((property) => {
      /*
       * NOTE: If hydrolysis can not parse a value, it is set to 'UNKNOWN'.
       * Unset any unknowns.
       */
      Object.getOwnPropertyNames(property).forEach(function(key) {
        if (property[key] === 'UNKNOWN') {
          delete property[key];
        }
      });

      /*
       * NOTE: Sometimes the property type comes in the lower case ('string')
       * instead of the class name case 'String'. Apply correction.
       */
      if (property.type.toLowerCase() === property.type) {
        property.type = _.upperFirst(property.type);
      }

      /*
       * NOTE: Some elements having IronFormElementBehavior skip declaring
       * `notify: true` for the value property in order to avoid firing the
       * notification event twice. Set notify for the value in such a case.
       *
       * NOTE: We can not detect IronFormElementBehavior when it is declared
       * deep in the dependant behaviors. Using the 'iron-form-element-register'
       * event presence to detect the behavior instead.
       */
      if (property.name === 'value'
          && this.events.find((event) => event.name === 'iron-form-element-register')) {
        property.notify = true;
      }

      // Extract public properties and methods to separate arrays
      if (!property.private) {
        this[property.function && property.params? 'publicMethods' : 'publicProperties'].push(property);
      }
    });

    // Remove duplicated properties and events
    this.publicProperties = _.uniqBy(this.publicProperties, 'name');

    /*
     * NOTE: Sometimes the event name contains following lines. See the
     * `transitionend` event in `<paper-button>` analysis for an example.
     */
    if (this.events) {
      this.events.forEach((event) => event.name = event.name.split('\n')[0]);
    }

    /*
     * NOTE: Some elements have property-changed events declared explicitely,
     * while also having the notify: true flag for the corresponding property.
     * We exclude such events from the list, because they are generated from the
     * properties data
     */
    this.events = this.events.filter((event) => {
      var changedEnding = /-changed$/;
      if (!changedEnding.test(event.name)) {
        return true;
      }
      var propertyName = _.camelCase(event.name.replace(changedEnding, ''));
      var property = this.publicProperties.find((property) => property.name === propertyName)
      if (property && property.notify) {
        return false;
      }
      return true;
    });

    // Remove duplicated events
    this.events = _.uniqBy(this.events, 'name');

    /*
     * Additional properties
     */
    this.name = this.getJavaName(this.is);
    this.bowerPackageName = this.path.split(path.sep)[0];
    this.package = this.getCollectionName(this.bowerPackageName);
    this.behaviorNames = (this.behaviors || []).map(this.getJavaName.bind(this));

    /*
     * Preprocessing
     */
    this._loadBowerData();
  }

  _loadBowerData() {
    var bowerFilePath = path.join(globalVar.bowerDir + this.bowerPackageName, 'bower.json');
    var bowerFileContent = fs.readFileSync(bowerFilePath);
    var bowerData = {}
    if (bowerFileContent) {
      try {
        this._bowerData = JSON.parse(bowerFileContent);
      } catch(e) {}
    }

    this.project = bowerData.name || 'unknown';
    this.license = bowerData.license || 'unknown';

    // List authors as Array<String>
    var authors = bowerData.authors;
    if (!Array.isArray(authors)) {
      authors = authors ? [authors] : [];
    }
    authors = authors.length ? authors : ['unknown author']
    this.authors = authors.map((author) => author.name || author);
  }
}
