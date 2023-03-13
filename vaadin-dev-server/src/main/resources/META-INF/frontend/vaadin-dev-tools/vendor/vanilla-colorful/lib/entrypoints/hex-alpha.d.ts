import { AlphaColorPicker } from '../components/alpha-color-picker.js';
import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap } from '../types';
export interface HexAlphaBase {
    addEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HexAlphaBase extends AlphaColorPicker<string> {
    protected get colorModel(): ColorModel<string>;
}
//# sourceMappingURL=hex-alpha.d.ts.map