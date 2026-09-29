/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A controller for linking a `<label>` element with an `<input>` element.
 */
export class LabelledInputController {
  constructor(input, labelController) {
    this.input = input;
    this.__preventDuplicateLabelClick = this.__preventDuplicateLabelClick.bind(this);

    labelController.addEventListener('slot-content-changed', (event) => {
      this.__initLabel(event.detail.node);
    });

    // Initialize the default label element
    this.__initLabel(labelController.node);
  }

  /**
   * @param {HTMLElement} label
   * @private
   */
  __initLabel(label) {
    if (label) {
      label.addEventListener('click', this.__preventDuplicateLabelClick);

      if (this.input) {
        label.setAttribute('for', this.input.id);
      }
    }
  }

  /**
   * The native platform fires an event for both the click on the label, and also
   * the subsequent click on the native input element caused by label click.
   * This results in two click events arriving at the host, but we only want one.
   * This method prevents the duplicate click and ensures the correct isTrusted event
   * with the correct event.target arrives at the host.
   * @private
   */
  __preventDuplicateLabelClick() {
    const inputClickHandler = (e) => {
      e.stopImmediatePropagation();
      this.input.removeEventListener('click', inputClickHandler);
    };
    this.input.addEventListener('click', inputClickHandler);
  }
}
