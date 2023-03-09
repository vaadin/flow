import { ColorPicker } from '../components/color-picker.js';
import { hsvStringToHsva, hsvaToHsvString } from '../utils/convert.js';
import { equalColorString } from '../utils/compare.js';
const colorModel = {
    defaultColor: 'hsv(0, 0%, 0%)',
    toHsva: hsvStringToHsva,
    fromHsva: hsvaToHsvString,
    equal: equalColorString,
    fromAttr: (color) => color
};
export class HsvStringBase extends ColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hsv-string.js.map