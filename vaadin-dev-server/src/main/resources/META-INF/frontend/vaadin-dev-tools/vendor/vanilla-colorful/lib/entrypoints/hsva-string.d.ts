import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap } from '../types';
import { AlphaColorPicker } from '../components/alpha-color-picker.js';
export interface HsvaStringBase {
    addEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HsvaStringBase extends AlphaColorPicker<string> {
    protected get colorModel(): ColorModel<string>;
}
//# sourceMappingURL=hsva-string.d.ts.map