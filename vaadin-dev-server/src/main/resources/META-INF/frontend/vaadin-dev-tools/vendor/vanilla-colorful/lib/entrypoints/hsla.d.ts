import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap, HslaColor } from '../types';
import { AlphaColorPicker } from '../components/alpha-color-picker.js';
export interface HslaBase {
    addEventListener<T extends keyof ColorPickerEventMap<HslaColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HslaColor>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<HslaColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HslaColor>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HslaBase extends AlphaColorPicker<HslaColor> {
    protected get colorModel(): ColorModel<HslaColor>;
}
//# sourceMappingURL=hsla.d.ts.map