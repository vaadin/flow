/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A controller for listening on media query changes.
 */
export class MediaQueryController {
  constructor(query, callback) {
    /**
     * The CSS media query to evaluate.
     *
     * @type {string}
     * @protected
     */
    this.query = query;

    /**
     * Function to call when media query changes.
     *
     * @type {Function}
     * @protected
     */
    this.callback = callback;

    this._boundQueryHandler = this._queryHandler.bind(this);
  }

  hostConnected() {
    this._removeListener();

    this._mediaQuery = window.matchMedia(this.query);

    this._addListener();

    this._queryHandler(this._mediaQuery);
  }

  hostDisconnected() {
    this._removeListener();
  }

  /** @private */
  _addListener() {
    if (this._mediaQuery) {
      this._mediaQuery.addListener(this._boundQueryHandler);
    }
  }

  /** @private */
  _removeListener() {
    if (this._mediaQuery) {
      this._mediaQuery.removeListener(this._boundQueryHandler);
    }

    this._mediaQuery = null;
  }

  /** @private */
  _queryHandler(mediaQuery) {
    if (typeof this.callback === 'function') {
      this.callback(mediaQuery.matches);
    }
  }
}
