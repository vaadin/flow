/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import './version.js';
import { css } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { addMaterialGlobalStyles } from './global.js';

const userColors = css`
  :host {
    --vaadin-user-color-0: #ab47bc;
    --vaadin-user-color-1: #546e7a;
    --vaadin-user-color-2: #2e7d32;
    --vaadin-user-color-3: #6d4c41;
    --vaadin-user-color-4: #1976d2;
    --vaadin-user-color-5: #00838f;
    --vaadin-user-color-6: #827717;
  }

  [theme~='dark'] {
    --vaadin-user-color-0: #9fa8da;
    --vaadin-user-color-1: #00bcd4;
    --vaadin-user-color-2: #ffeb3b;
    --vaadin-user-color-3: #a1887f;
    --vaadin-user-color-4: #2196f3;
    --vaadin-user-color-5: #4caf50;
    --vaadin-user-color-6: #ff9800;
  }
`;

addMaterialGlobalStyles('user-colors', userColors);

export { userColors };
