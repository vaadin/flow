import { HsvBase } from './lib/entrypoints/hsv.js';
export type { HsvColor } from './lib/types';
/**
 * A color picker custom element that uses HSV object format.
 *
 * @element hsv-color-picker
 *
 * @prop {HsvColor} color - Selected color in HSV object format.
 *
 * @fires color-changed - Event fired when color property changes.
 *
 * @csspart hue - A hue selector container.
 * @csspart saturation - A saturation selector container
 * @csspart hue-pointer - A hue pointer element.
 * @csspart saturation-pointer - A saturation pointer element.
 */
export declare class HsvColorPicker extends HsvBase {
}
declare global {
    interface HTMLElementTagNameMap {
        'hsv-color-picker': HsvColorPicker;
    }
}
//# sourceMappingURL=hsv-color-picker.d.ts.map