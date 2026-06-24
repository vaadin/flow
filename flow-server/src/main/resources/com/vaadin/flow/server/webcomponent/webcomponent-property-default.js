/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
if (this.hasOwnProperty('_PropertyName_')) {
  this['__PropertyName_'] = this['_PropertyName_'];
  delete this['_PropertyName_'];
} else {
  if (_DefaultValue_ !== undefined) {
    this['__PropertyName_'] = _DefaultValue_;
  }
}
