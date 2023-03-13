import { ColorPicker } from '../components/color-picker.js';
import { hslStringToHsva, hsvaToHslString } from '../utils/convert.js';
import { equalColorString } from '../utils/compare.js';
const colorModel = {
    defaultColor: 'hsl(0, 0%, 0%)',
    toHsva: hslStringToHsva,
    fromHsva: hsvaToHslString,
    equal: equalColorString,
    fromAttr: (color) => color
};
export class HslStringBase extends ColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hsl-string.js.map