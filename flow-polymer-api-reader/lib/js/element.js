'use strict';

var Behavior = require('./behavior');

module.exports = class Element extends Behavior {
  constructor(filePath, descriptor) {
    super(filePath, descriptor);

    /*
     * Default properties existing in all elements
     */
    this.publicProperties.unshift({
      name: 'hidden',
      type: 'Boolean',
      default: 'false'
    });

    /*
     * Default events existing in all elements
     */
    if (!this.events) {
      this.events = [];
    }
    this.events.unshift({
      name: 'click'
    });
  }
}
