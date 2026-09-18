/**
 * @license
 * Copyright (c) 2017 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import '../color.js';
import '../font-icons.js';
import '../typography.js';
import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { fieldButton } from './field-button.js';
import { helper } from './helper.js';
import { requiredField } from './required-field.js';

const inputField = css`
  :host {
    position: relative;
    padding-top: 8px;
    margin-bottom: 8px;
    color: var(--material-body-text-color);
    font-size: var(--material-body-font-size);
    line-height: 24px;
    font-family: var(--material-font-family);
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  :host::before {
    line-height: 32px;
  }

  /* Strange gymnastics to make fields vertically align nicely in most cases
     (no label, with label, without prefix, with prefix, etc.) */

  :host([has-label]) {
    padding-top: 24px;
  }

  :host([has-label]) ::slotted([slot='tooltip']) {
    --vaadin-tooltip-offset-bottom: -8px;
  }

  [part='input-field'] {
    position: relative;
    top: -0.2px; /* NOTE(platosha): Adjusts for wrong flex baseline in Chrome & Safari */
    height: 32px;
    padding-left: 0;
    padding-right: 0;
    background-color: transparent;
    margin: 0;
  }

  [part='input-field']::before,
  [part='input-field']::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    transform-origin: 50% 0%;
    background-color: var(--_material-text-field-input-line-background-color, #000);
    opacity: var(--_material-text-field-input-line-opacity, 0.42);
  }

  [part='input-field']::after {
    background-color: var(--material-primary-color);
    opacity: 0;
    height: 2px;
    bottom: 0;
    transform: scaleX(0);
    transition: opacity 0.175s;
  }

  :host([disabled]) [part='label'],
  :host([disabled]) [part='input-field'] ::slotted(:is(input, textarea)) {
    color: var(--material-disabled-text-color);
    -webkit-text-fill-color: var(--material-disabled-text-color);
  }

  [part='input-field'] ::slotted(:is(input, textarea)) {
    outline: none;
    margin: 0;
    border: 0;
    border-radius: 0;
    padding: 8px 0;
    width: 100%;
    height: 100%;
    font-family: inherit;
    font-size: 1em;
    line-height: inherit;
    color: inherit;
    background-color: transparent;
    /* Disable default invalid style in Firefox */
    box-shadow: none;
  }

  /* TODO: the text opacity should be 42%, but the disabled style is 38%.
  Would need to introduce another property for it if we want to be 100% accurate. */
  ::slotted(:is(input, textarea):placeholder-shown) {
    color: var(--material-disabled-text-color);
    transition: opacity 0.175s 0.1s;
  }

  /* prettier-ignore */
  :host([has-label]:not([focused]):not([invalid]):not([theme='always-float-label'])) ::slotted(:is(input, textarea):placeholder-shown) {
    opacity: 0;
    transition-delay: 0;
  }

  [part='label'] {
    width: 133%;
    transition:
      transform 0.175s,
      color 0.175s,
      width 0.175s;
    transition-timing-function: ease, ease, step-end;
  }

  :host(:hover:not([readonly]):not([invalid])) [part='input-field']::before {
    opacity: var(--_material-text-field-input-line-hover-opacity, 0.87);
  }

  :host([focused]:not([invalid])) [part='label'] {
    color: var(--material-primary-text-color);
  }

  :host([focused]) [part='input-field']::after,
  :host([invalid]) [part='input-field']::after {
    opacity: 1;
    transform: none;
    transition:
      transform 0.175s,
      opacity 0.175s;
  }

  :host([invalid]) [part='input-field']::after {
    background-color: var(--material-error-color);
  }

  :host([input-prevented]) [part='input-field'] {
    color: var(--material-error-text-color);
  }

  :host([disabled]) {
    pointer-events: none;
  }

  :host([disabled]) [part='input-field'] {
    color: var(--material-disabled-text-color);
  }

  :host([disabled]) [part='input-field']::before {
    background-color: transparent;
    background-image: linear-gradient(
      90deg,
      var(--_material-text-field-input-line-background-color, #000) 0,
      var(--_material-text-field-input-line-background-color, #000) 2px,
      transparent 2px
    );
    background-size: 4px 1px;
    background-repeat: repeat-x;
  }

  /* Only target the visible floating label */
  :host([has-label]:not([has-value]):not([focused]):not([invalid]):not([theme~='always-float-label'])) [part='label'] {
    width: 100%;
    transform: scale(1) translateY(24px);
    transition-timing-function: ease, ease, step-start;
    pointer-events: none;
    left: auto;
    right: auto;
    transition-delay: 0.1s;
  }

  /* Slotted content */
  [part='input-field'] ::slotted(*:not([part$='-button']):not(input):not(textarea)) {
    color: var(--material-secondary-text-color);
  }

  [part='clear-button']::before {
    content: var(--material-icons-clear);
  }

  /* RTL specific styles */

  :host([disabled][dir='rtl']) [part='input-field']::before {
    background-image: linear-gradient(
      -90deg,
      var(--_material-text-field-input-line-background-color, #000) 0,
      var(--_material-text-field-input-line-background-color, #000) 2px,
      transparent 2px
    );
  }
`;

const inputFieldShared = [requiredField, fieldButton, helper, inputField];

registerStyles('', inputFieldShared, {
  moduleId: 'material-input-field-shared-styles',
});

export { inputField, inputFieldShared };
