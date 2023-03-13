import { ColorPicker } from '../components/color-picker.js';
import { hexToHsva, hsvaToHex } from '../utils/convert.js';
import { equalHex } from '../utils/compare.js';
const colorModel = {
    defaultColor: '#000',
    toHsva: hexToHsva,
    fromHsva: ({ h, s, v }) => hsvaToHex({ h, s, v, a: 1 }),
    equal: equalHex,
    fromAttr: (color) => color
};
export class HexBase extends ColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hex.js.map