import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap } from '../types';
import { AlphaColorPicker } from '../components/alpha-color-picker.js';
export interface HslaStringBase {
    addEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HslaStringBase extends AlphaColorPicker<string> {
    protected get colorModel(): ColorModel<string>;
}
//# sourceMappingURL=hsla-string.d.ts.map