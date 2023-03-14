import { HslBase } from './lib/entrypoints/hsl.js';
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
export class HslColorPicker extends HslBase {
}
customElements.define('hsl-color-picker', HslColorPicker);
//# sourceMappingURL=hsl-color-picker.js.map