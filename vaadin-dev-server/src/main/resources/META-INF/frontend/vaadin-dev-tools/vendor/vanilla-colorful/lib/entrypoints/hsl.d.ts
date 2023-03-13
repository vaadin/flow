import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap, HslColor } from '../types';
import { ColorPicker } from '../components/color-picker.js';
export interface HslBase {
    addEventListener<T extends keyof ColorPickerEventMap<HslColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HslColor>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<HslColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HslColor>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HslBase extends ColorPicker<HslColor> {
    protected get colorModel(): ColorModel<HslColor>;
}
//# sourceMappingURL=hsl.d.ts.map