import { html, LitElement, css } from "lit";

class SimpleObserver extends LitElement {
  render() {
    return html`
      <vaadin-vertical-layout>
        <span>First: ${this.first} (last value: ${this.previousFirst})</span>
        <span>Last: ${this.last} (last value: ${this.previousLast})</span>
      </vaadin-vertical-layout>
    `;
  }

  static get properties() {
    return {
      first: { type: String },
      last: { type: String },
      previousFirst: String,
      previousLast: String,
      example: {
        type: String,
        /* TODO: Convert this complex observer manually */
        observer: "userListChanged(users.*, filter)",
      },
    };
  }

  _firstChanged(newFirst, oldFirst) {
    this.previousFirst = oldFirst;
  }

  _lastChanged(newLast, oldLast) {
    this.previousLast = oldLast;
  }

  static get is() {
    return "simple-observer";
  }
  set first(newValue) {
    const oldValue = this.first;
    this._first = newValue;
    if (oldValue !== newValue) {
      this._firstChanged(newValue, oldValue);
      this.requestUpdateInternal(
        "first",
        oldValue,
        this.constructor.properties.first
      );
    }
  }
  get first() {
    return this._first;
  }

  set last(newValue) {
    const oldValue = this.last;
    this._last = newValue;
    if (oldValue !== newValue) {
      this._lastChanged(newValue, oldValue);
      this.requestUpdateInternal(
        "last",
        oldValue,
        this.constructor.properties.last
      );
    }
  }
  get last() {
    return this._last;
  }
}

customElements.define(SimpleObserver.is, SimpleObserver);
