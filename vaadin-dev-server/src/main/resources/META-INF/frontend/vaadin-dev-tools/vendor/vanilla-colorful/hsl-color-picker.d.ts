import { HslBase } from './lib/entrypoints/hsl.js';
export type { HslColor } from './lib/types';
/**
 * A color picker custom element that uses HSL object format.
 *
 * @element hsl-color-picker
 *
 * @prop {HslColor} color - Selected color in HSL object format.
 *
 * @fires color-changed - Event fired when color property changes.
 *
 * @csspart hue - A hue selector container.
 * @csspart saturation - A saturation selector container
 * @csspart hue-pointer - A hue pointer element.
 * @csspart saturation-pointer - A saturation pointer element.
 */
export declare class HslColorPicker extends HslBase {
}
declare global {
    interface HTMLElementTagNameMap {
        'hsl-color-picker': HslColorPicker;
    }
}
//# sourceMappingURL=hsl-color-picker.d.ts.map