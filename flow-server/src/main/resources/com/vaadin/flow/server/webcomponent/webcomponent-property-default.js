if (this.hasOwnProperty('_PropertyName_')) {
  this['__PropertyName_'] = this['_PropertyName_'];
  delete this['_PropertyName_'];
} else {
  if (_DefaultValue_ !== undefined) {
    this['__PropertyName_'] = _DefaultValue_;
  }
}
