import { HsvBase } from './lib/entrypoints/hsv.js';
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
export class HsvColorPicker extends HsvBase {
}
customElements.define('hsv-color-picker', HsvColorPicker);
//# sourceMappingURL=hsv-color-picker.js.map