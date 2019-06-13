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
    if (this.$server) {
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
          console.debug('reconnecting '+this+' using id '+this.$.id);
          this.$server.reconnect();
      }
  }

  _registerElement() {
    this.$.id = "_TagCamel_-" + _TagCamel_.id++;
    console.debug('registering '+this+' using id '+this.$.id);
    const flowRoot = document.body;

    const poller = () => {
      var flowClient = Object.values(window.Vaadin.Flow.clients).find(c => c.exportedWebComponents && c.exportedWebComponents.indexOf('_TagDash_') != -1);
      if (flowClient && flowClient.connectWebComponent) {
        flowRoot.$ = flowRoot.$ || {};
        flowRoot.$[this.$.id] = this;
        flowClient.connectWebComponent({tag: '_TagDash_', id: this.$.id});
        console.debug('connected '+this+' using id '+this.$.id);
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
    Object.keys(_TagCamel_.properties).forEach(prop => {
      if (prop !== "_propertyUpdatedFromServer") {
        this._sync(prop, this[prop]);
      }
    });
  }
}

_TagCamel_.id = 0;
customElements.define(_TagCamel_.is, _TagCamel_);
