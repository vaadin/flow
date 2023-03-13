import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap } from '../types';
import { ColorPicker } from '../components/color-picker.js';
export interface RgbStringBase {
    addEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class RgbStringBase extends ColorPicker<string> {
    protected get colorModel(): ColorModel<string>;
}
//# sourceMappingURL=rgb-string.d.ts.map