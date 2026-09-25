/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import '@vaadin/component-base/src/styles/style-props.js';
import { css, unsafeCSS } from 'lit';

// postcss-lit-disable-next-line
export const checkable = (part, propName = part) => css`
  :host {
    align-items: baseline;
    column-gap: var(--vaadin-${unsafeCSS(propName)}-gap, var(--vaadin-gap-s));
    grid-template: none;
    grid-template-columns: auto 1fr;
    grid-template-rows: repeat(auto-fill, minmax(0, max-content));
    -webkit-tap-highlight-color: transparent;
    --_cursor: var(--vaadin-clickable-cursor);
  }

  :host([disabled]) {
    --_cursor: var(--vaadin-disabled-cursor);
  }

  :host(:not([has-label])) {
    column-gap: 0;
  }

  [part='${unsafeCSS(part)}'],
  ::slotted(input),
  [part='label'],
  ::slotted(label) {
    grid-row: 1;
  }

  [part='label'],
  ::slotted(label) {
    font-size: var(--vaadin-${unsafeCSS(propName)}-label-font-size, var(--vaadin-input-field-label-font-size, inherit));
    line-height: var(--vaadin-${unsafeCSS(propName)}-label-line-height, var(--vaadin-input-field-label-line-height, inherit));
    font-weight: var(--vaadin-${unsafeCSS(propName)}-label-font-weight, var(--vaadin-input-field-label-font-weight, 500));
    color: var(--vaadin-${unsafeCSS(propName)}-label-color, var(--vaadin-input-field-label-color, var(--vaadin-text-color)));
    word-break: break-word;
    cursor: var(--_cursor);
  }

  [part='${unsafeCSS(part)}'],
  ::slotted(input) {
    grid-column: 1;
  }

  [part='label'],
  [part='helper-text'],
  [part='error-message'] {
    margin-bottom: 0;
    grid-column: 2;
    width: auto;
    min-width: auto;
  }

  [part='helper-text'],
  [part='error-message'] {
    margin-top: var(--_gap-s);
    grid-row: auto;
  }

  /* Baseline vertical alignment */
  :host::before {
    grid-row: 1;
    margin: 0;
    padding: 0;
    border: 0;
  }

  /* visually hidden */
  ::slotted(input) {
    cursor: inherit;
    align-self: stretch;
    appearance: none;
    cursor: var(--_cursor);
    /* Ensure minimum click target (WCAG) */
    margin: min(0px, (24px - 100%) / -2) !important;
    /* Extend the input to cover the gap between the checkbox/radio and label */
    margin-inline-end: calc(min(0px, (24px - 100%) / -2) - var(--vaadin-${unsafeCSS(propName)}-gap, var(--vaadin-gap-s))) !important;
  }

  /* Control container (checkbox, radio button) */
  [part='${unsafeCSS(part)}'] {
    background: var(--vaadin-${unsafeCSS(propName)}-background, var(--vaadin-background-color));
    border-color: var(--vaadin-${unsafeCSS(propName)}-border-color, var(--vaadin-input-field-border-color, var(--vaadin-border-color)));
    border-radius: var(--vaadin-${unsafeCSS(propName)}-border-radius, var(--vaadin-radius-s));
    border-style: var(--_border-style, solid);
    --_border-width: var(--vaadin-${unsafeCSS(propName)}-border-width, var(--vaadin-input-field-border-width, 1px));
    border-width: var(--_border-width);
    box-sizing: border-box;
    --_color: var(--vaadin-${unsafeCSS(propName)}-marker-color, var(--vaadin-${unsafeCSS(propName)}-background, var(--vaadin-background-color)));
    color: var(--_color);
    height: var(--vaadin-${unsafeCSS(propName)}-size, 1lh);
    width: var(--vaadin-${unsafeCSS(propName)}-size, 1lh);
    position: relative;
    cursor: var(--_cursor);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  :host(:is([checked], [indeterminate])) {
    --vaadin-${unsafeCSS(propName)}-background: var(--vaadin-text-color);
    --vaadin-${unsafeCSS(propName)}-border-color: transparent;
  }

  :host([disabled]) {
    --vaadin-${unsafeCSS(propName)}-background: var(--vaadin-input-field-disabled-background, var(--vaadin-background-container-strong));
    --vaadin-${unsafeCSS(propName)}-border-color: transparent;
    --vaadin-${unsafeCSS(propName)}-marker-color: var(--vaadin-text-color-disabled);
  }

  /* Focus ring */
  :host([focus-ring]) [part='${unsafeCSS(part)}'] {
    outline: var(--vaadin-focus-ring-width) solid var(--vaadin-focus-ring-color);
    outline-offset: calc(var(--_border-width) * -1);
  }

  :host([focus-ring]:is([checked], [indeterminate])) [part='${unsafeCSS(part)}'] {
    outline-offset: 1px;
  }

  :host([readonly][focus-ring]) [part='${unsafeCSS(part)}'] {
    --vaadin-${unsafeCSS(propName)}-border-color: transparent;
    outline-offset: calc(var(--_border-width) * -1);
    outline-style: dashed;
  }

  /* Checked indicator (checkmark, dot) */
  [part='${unsafeCSS(part)}']::after {
    content: '\\2003' / '';
    background: currentColor;
    border-radius: inherit;
    display: flex;
    align-items: center;
    --_filter: var(--vaadin-${unsafeCSS(propName)}-marker-color, saturate(0) invert(1) hue-rotate(180deg) contrast(100) brightness(100));
    filter: var(--_filter);
  }

  :host(:not([checked], [indeterminate])) [part='${unsafeCSS(part)}']::after {
    opacity: 0;
  }

  @media (forced-colors: active) {
    :host(:is([checked], [indeterminate])) {
      --vaadin-${unsafeCSS(propName)}-border-color: CanvasText !important;
    }

    :host(:is([checked], [indeterminate])) [part='${unsafeCSS(part)}'] {
      background: SelectedItem !important;
    }

    :host(:is([checked], [indeterminate])) [part='${unsafeCSS(part)}']::after {
      background: SelectedItemText !important;
    }

    :host([readonly]) [part='${unsafeCSS(part)}']::after {
      background: CanvasText !important;
    }

    :host([disabled]) {
      --vaadin-${unsafeCSS(propName)}-border-color: GrayText !important;
    }

    :host([disabled]) [part='${unsafeCSS(part)}']::after {
      background: GrayText !important;
    }
  }
`;
