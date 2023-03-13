import { AlphaColorPicker } from '../components/alpha-color-picker.js';
import { hexToHsva, hsvaToHex } from '../utils/convert.js';
import { equalHex } from '../utils/compare.js';
const colorModel = {
    defaultColor: '#0001',
    toHsva: hexToHsva,
    fromHsva: hsvaToHex,
    equal: equalHex,
    fromAttr: (color) => color
};
export class HexAlphaBase extends AlphaColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hex-alpha.js.map