import '@vaadin/vaadin-material-styles/color.js';
import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

registerStyles(
  'vaadin-input-container',
  css`
    :host {
      position: relative;
      top: -0.2px; /* NOTE(platosha): Adjusts for wrong flex baseline in Chrome & Safari */
      height: 32px;
      padding-left: 0;
      padding-right: 0;
      background-color: transparent;
      margin: 0;
    }

    :host::before,
    :host::after {
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

    :host::after {
      background-color: var(--material-primary-color);
      opacity: 0;
      height: 2px;
      bottom: 0;
      transform: scaleX(0);
      transition: opacity 0.175s;
    }

    ::slotted(:not([slot$='fix'])) {
      padding: 8px 0;
    }

    ::slotted([slot$='fix']) {
      color: var(--material-secondary-text-color);
    }

    /* Disabled */
    :host([disabled]) {
      color: var(--material-disabled-text-color);
    }

    :host([disabled])::before {
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

    :host([disabled]) ::slotted(:not([slot$='fix'])) {
      color: var(--material-disabled-text-color);
      -webkit-text-fill-color: var(--material-disabled-text-color);
    }

    /* Invalid */
    :host([invalid])::after {
      background-color: var(--material-error-color);
      opacity: 1;
      transform: none;
      transition:
        transform 0.175s,
        opacity 0.175s;
    }
  `,
  { moduleId: 'material-input-container' },
);
