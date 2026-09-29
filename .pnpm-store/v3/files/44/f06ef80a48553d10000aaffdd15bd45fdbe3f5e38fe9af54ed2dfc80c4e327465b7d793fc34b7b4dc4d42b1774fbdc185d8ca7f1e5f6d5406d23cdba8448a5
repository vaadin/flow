/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { css, unsafeCSS } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { addLumoGlobalStyles } from './global.js';
import { typography } from './typography.js';

const typographyWithoutHost = css`
  ${unsafeCSS(typography.cssText.replace(/,\s*:host/su, ''))}
`;
addLumoGlobalStyles('typography', typographyWithoutHost);
