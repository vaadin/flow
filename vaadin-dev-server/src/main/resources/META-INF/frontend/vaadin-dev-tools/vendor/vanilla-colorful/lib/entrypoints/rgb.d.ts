import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap, RgbColor } from '../types';
import { ColorPicker } from '../components/color-picker.js';
export interface RgbBase {
    addEventListener<T extends keyof ColorPickerEventMap<RgbColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<RgbColor>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<RgbColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<RgbColor>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class RgbBase extends ColorPicker<RgbColor> {
    protected get colorModel(): ColorModel<RgbColor>;
}
//# sourceMappingURL=rgb.d.ts.map