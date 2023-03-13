import { AlphaColorPicker } from '../components/alpha-color-picker.js';
import { hslaStringToHsva, hsvaToHslaString } from '../utils/convert.js';
import { equalColorString } from '../utils/compare.js';
const colorModel = {
    defaultColor: 'hsla(0, 0%, 0%, 1)',
    toHsva: hslaStringToHsva,
    fromHsva: hsvaToHslaString,
    equal: equalColorString,
    fromAttr: (color) => color
};
export class HslaStringBase extends AlphaColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hsla-string.js.map