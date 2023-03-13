import { AlphaColorPicker } from '../components/alpha-color-picker.js';
import { rgbaToHsva, hsvaToRgba } from '../utils/convert.js';
import { equalColorObjects } from '../utils/compare.js';
const colorModel = {
    defaultColor: { r: 0, g: 0, b: 0, a: 1 },
    toHsva: rgbaToHsva,
    fromHsva: hsvaToRgba,
    equal: equalColorObjects,
    fromAttr: (color) => JSON.parse(color)
};
export class RgbaBase extends AlphaColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=rgba.js.map