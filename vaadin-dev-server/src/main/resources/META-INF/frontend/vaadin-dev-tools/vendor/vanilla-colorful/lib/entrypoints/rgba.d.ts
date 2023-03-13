import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap, RgbaColor } from '../types';
import { AlphaColorPicker } from '../components/alpha-color-picker.js';
export interface RgbaBase {
    addEventListener<T extends keyof ColorPickerEventMap<RgbaColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<RgbaColor>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<RgbaColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<RgbaColor>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class RgbaBase extends AlphaColorPicker<RgbaColor> {
    protected get colorModel(): ColorModel<RgbaColor>;
}
//# sourceMappingURL=rgba.d.ts.map