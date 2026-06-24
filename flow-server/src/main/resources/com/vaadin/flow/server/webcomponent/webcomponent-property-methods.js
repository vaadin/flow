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
