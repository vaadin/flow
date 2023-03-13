import { RgbaStringBase } from './lib/entrypoints/rgba-string.js';
/**
 * A color picker custom element that uses RGBA string format.
 *
 * @element rgba-string-color-picker
 *
 * @prop {string} color - Selected color in RGBA string format.
 * @attr {string} color - Selected color in RGBA string format.
 *
 * @fires color-changed - Event fired when color property changes.
 *
 * @csspart hue - A hue selector container.
 * @csspart saturation - A saturation selector container
 * @csspart alpha - An alpha selector container.
 * @csspart hue-pointer - A hue pointer element.
 * @csspart saturation-pointer - A saturation pointer element.
 * @csspart alpha-pointer - An alpha pointer element.
 */
export declare class RgbaStringColorPicker extends RgbaStringBase {
}
declare global {
    interface HTMLElementTagNameMap {
        'rgba-string-color-picker': RgbaStringColorPicker;
    }
}
//# sourceMappingURL=rgba-string-color-picker.d.ts.map