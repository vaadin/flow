/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
set ['_PropertyName_'](value) {
  if (this['__PropertyName_'] === value)
    return;
  this['__PropertyName_'] = value;
  this._sync('_PropertyName_', value);
  var eventDetails = { value: value };
  var eventName = '_ChangeEventName_';
  this.dispatchEvent(new CustomEvent(eventName, eventDetails));
}

get ['_PropertyName_']() {
  return this['__PropertyName_'];
}
