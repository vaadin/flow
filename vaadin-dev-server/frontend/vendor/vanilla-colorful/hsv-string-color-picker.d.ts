import { HsvStringBase } from './lib/entrypoints/hsv-string.js';
/**
 * A color picker custom element that uses HSV string format.
 *
 * @element hsv-string-color-picker
 *
 * @prop {string} color - Selected color in HSV string format.
 * @attr {string} color - Selected color in HSV string format.
 *
 * @fires color-changed - Event fired when color property changes.
 *
 * @csspart hue - A hue selector container.
 * @csspart saturation - A saturation selector container
 * @csspart hue-pointer - A hue pointer element.
 * @csspart saturation-pointer - A saturation pointer element.
 */
export declare class HsvStringColorPicker extends HsvStringBase {
}
declare global {
    interface HTMLElementTagNameMap {
        'hsv-string-color-picker': HsvStringColorPicker;
    }
}
//# sourceMappingURL=hsv-string-color-picker.d.ts.map