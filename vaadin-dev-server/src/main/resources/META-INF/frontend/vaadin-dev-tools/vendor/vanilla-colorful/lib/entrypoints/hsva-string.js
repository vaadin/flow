import { AlphaColorPicker } from '../components/alpha-color-picker.js';
import { hsvaStringToHsva, hsvaToHsvaString } from '../utils/convert.js';
import { equalColorString } from '../utils/compare.js';
const colorModel = {
    defaultColor: 'hsva(0, 0%, 0%, 1)',
    toHsva: hsvaStringToHsva,
    fromHsva: hsvaToHsvaString,
    equal: equalColorString,
    fromAttr: (color) => color
};
export class HsvaStringBase extends AlphaColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hsva-string.js.map