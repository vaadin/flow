  static get is() {
    return '_TagDash_';
  }

  static get properties() {
    return Object.assign({},
      _Properties_,
      {
        _propertyUpdatedFromServer: {
          type: Object,
          notify: false,
          value: {},
        }
      });
  }

  _PropertyMethods_

  _sync(property, newValue) {
    // Don't sync, if the web component has not been registered yet
    if (this.$server && document.body.$[this.$.id] === this) {
      if (!this._propertyUpdatedFromServer[property]) {
        this.$server.sync(property, newValue);
      } else {
        this._propertyUpdatedFromServer[property] = false;
      }
    }
  }

  _updatePropertyFromServer(property, newValue) {
    if (this.__proto__.hasOwnProperty(property)) {
      this._propertyUpdatedFromServer[property] = true;
      this[property] = newValue;
    }
  }

  connectedCallback() {
    super.connectedCallback();

    this._connect();
  }

  _connect(){
      if (!this.$.id) {
        this._registerElement();
      } else {
        this.$server.reconnect();
      }
      console.debug('connected', this);
  }

  _registerElement() {
    this.$.id = "_TagCamel_-" + _TagCamel_.id++;
    const flowRoot = document.body;
    // Needed to make Flow do lookup correctly
    const poller = () => {
      if (flowRoot && flowRoot.$server) {
        flowRoot.$ = flowRoot.$ || {};
        flowRoot.$[this.$.id] = this;
        const properties = {};
        for (var prop in _TagCamel_.properties) {
          if (prop === "_propertyUpdatedFromServer") {
            continue;
          }
          properties[prop] = this[prop];
        }
        flowRoot.$server.connectWebComponent('_TagDash_', this.$.id, properties);
      } else {
        setTimeout(poller, 10);
      }
    };

    poller();
  }

  disconnectedCallback() {
    this.$server.disconnected();

    console.log('disconnected', this);
  }

  serverConnected() {
  }
}

_TagCamel_.id = 0;

customElements.define(_TagCamel_.is, _TagCamel_);
