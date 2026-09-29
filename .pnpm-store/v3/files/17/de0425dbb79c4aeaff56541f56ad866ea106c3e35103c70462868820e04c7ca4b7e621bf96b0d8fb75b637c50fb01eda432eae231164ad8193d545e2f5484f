/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';
import { ResizeMixin } from '@vaadin/component-base/src/resize-mixin.js';
import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { needsFontIconSizingFallback } from './vaadin-icon-helpers.js';

const usesFontIconSizingFallback = needsFontIconSizingFallback();

if (usesFontIconSizingFallback) {
  registerStyles(
    'vaadin-icon',
    css`
      :host::after,
      :host::before {
        font-size: var(--_vaadin-font-icon-size);
      }
    `,
    'vaadin-icon-font-size-mixin-styles',
  );
}

/**
 * Mixin which enables the font icon sizing fallback for browsers that do not support CSS Container Queries.
 * The mixin does nothing if the browser supports CSS Container Query units for pseudo elements.
 *
 * @polymerMixin
 */
export const IconFontSizeMixin = dedupingMixin((superclass) =>
  !usesFontIconSizingFallback
    ? superclass
    : class extends ResizeMixin(superclass) {
        static get observers() {
          return ['__iconFontSizeMixinfontChanged(iconClass, char, ligature)'];
        }

        /** @protected */
        ready() {
          super.ready();

          // Update once initially to avoid a fouc
          this.__updateFontIconSize();
        }

        /** @private */
        __iconFontSizeMixinfontChanged(_iconClass, _char, _ligature) {
          // Update when iconClass, char or ligature changes
          this.__updateFontIconSize();
        }

        /**
         * @protected
         * @override
         */
        _onResize() {
          // Update when the element is resized
          this.__updateFontIconSize();
        }

        /**
         * Updates the --_vaadin-font-icon-size CSS variable value if font icons are used.
         *
         * @private
         */
        __updateFontIconSize() {
          if (this.char || this.iconClass || this.ligature) {
            const { paddingTop, paddingBottom, height } = getComputedStyle(this);
            const fontIconSize = parseFloat(height) - parseFloat(paddingTop) - parseFloat(paddingBottom);
            this.style.setProperty('--_vaadin-font-icon-size', `${fontIconSize}px`);
          }
        }
      },
);
