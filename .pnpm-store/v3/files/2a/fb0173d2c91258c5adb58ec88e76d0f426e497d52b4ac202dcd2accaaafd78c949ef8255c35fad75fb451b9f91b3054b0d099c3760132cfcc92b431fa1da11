/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { css } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

export const iconStyles = css`
  :host {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    box-sizing: border-box;
    vertical-align: middle;
    width: 24px;
    height: 24px;
    fill: currentColor;
    container-type: size;
  }

  :host::after,
  :host::before {
    line-height: 1;
    font-size: 100cqh;
    -webkit-font-smoothing: antialiased;
    text-rendering: optimizeLegibility;
    -moz-osx-font-smoothing: grayscale;
  }

  :host([hidden]) {
    display: none !important;
  }

  svg {
    display: block;
    width: 100%;
    height: 100%;
    /* prevent overflowing icon from clipping, see https://github.com/vaadin/flow-components/issues/5872 */
    overflow: visible;
  }

  :host(:is([icon-class], [font-icon-content])) svg {
    display: none;
  }

  :host([font-icon-content])::before {
    content: attr(font-icon-content);
  }
`;
