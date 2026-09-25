/**
 * @license
 * Copyright (c) 2017 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { css } from 'lit';
import { addGlobalStyles } from '../css-utils.js';

addGlobalStyles(
  'vaadin-base-user-colors',
  css`
    @layer vaadin.base {
      html {
        --_color-count: 10;
        --_hue-step: round(360 / var(--_color-count), 1);
        --vaadin-user-color-0: var(--vaadin-user-color, oklch(0.52 0.2 240));
        --vaadin-user-color-1: oklch(
          from var(--vaadin-user-color-0) calc(0.62 + clamp(-0.15, (0.6201 - l) * 10000, 0.15)) c
            calc(h - var(--_hue-step) * 2 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-2: oklch(
          from var(--vaadin-user-color-0) l c calc(h - var(--_hue-step) * -2 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-3: oklch(
          from var(--vaadin-user-color-0) calc(0.62 + clamp(-0.15, (0.6201 - l) * 10000, 0.15)) c
            calc(h - var(--_hue-step) * 0 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-4: oklch(
          from var(--vaadin-user-color-0) l c calc(h - var(--_hue-step) * 2 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-5: oklch(
          from var(--vaadin-user-color-0) calc(0.62 + clamp(-0.15, (0.6201 - l) * 10000, 0.15)) c
            calc(h - var(--_hue-step) * -2 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-6: oklch(
          from var(--vaadin-user-color-0) l c calc(h - var(--_hue-step) * -4 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-7: oklch(
          from var(--vaadin-user-color-0) calc(0.62 + clamp(-0.15, (0.6201 - l) * 10000, 0.15)) c
            calc(h - var(--_hue-step) * 4 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-8: oklch(
          from var(--vaadin-user-color-0) l c calc(h - var(--_hue-step) * 4 * var(--_vaadin-safari-17-deg, 1))
        );
        --vaadin-user-color-9: oklch(
          from var(--vaadin-user-color-0) calc(0.62 + clamp(-0.15, (0.6201 - l) * 10000, 0.15)) c
            calc(h - var(--_hue-step) * 6 * var(--_vaadin-safari-17-deg, 1))
        );
      }
    }
  `,
);
