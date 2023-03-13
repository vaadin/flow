import { ColorPicker, Sliders, $css, $sliders } from './color-picker.js';
import type { AnyColor } from '../types';
export declare abstract class AlphaColorPicker<C extends AnyColor> extends ColorPicker<C> {
    protected get [$css](): string[];
    protected get [$sliders](): Sliders;
}
//# sourceMappingURL=alpha-color-picker.d.ts.map