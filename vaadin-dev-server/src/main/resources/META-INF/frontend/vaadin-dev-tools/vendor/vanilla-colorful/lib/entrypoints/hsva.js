import { AlphaColorPicker } from '../components/alpha-color-picker.js';
import { equalColorObjects } from '../utils/compare.js';
import { roundHsva } from '../utils/convert.js';
const colorModel = {
    defaultColor: { h: 0, s: 0, v: 0, a: 1 },
    toHsva: (hsva) => hsva,
    fromHsva: roundHsva,
    equal: equalColorObjects,
    fromAttr: (color) => JSON.parse(color)
};
export class HsvaBase extends AlphaColorPicker {
    get colorModel() {
        return colorModel;
    }
}
//# sourceMappingURL=hsva.js.map