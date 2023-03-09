import { HexAlphaBase } from './lib/entrypoints/hex-alpha.js';
/**
 * A color picker custom element that uses HEX format with alpha.
 *
 * @element hex-alpha-color-picker
 *
 * @prop {string} color - Selected color in HEX format.
 * @attr {string} color - Selected color in HEX format.
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
export class HexAlphaColorPicker extends HexAlphaBase {
}
customElements.define('hex-alpha-color-picker', HexAlphaColorPicker);
//# sourceMappingURL=hex-alpha-color-picker.js.map