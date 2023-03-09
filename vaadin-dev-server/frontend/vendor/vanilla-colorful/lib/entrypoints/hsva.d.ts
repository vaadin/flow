import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap, HsvaColor } from '../types';
import { AlphaColorPicker } from '../components/alpha-color-picker.js';
export interface HsvaBase {
    addEventListener<T extends keyof ColorPickerEventMap<HsvaColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HsvaColor>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<HsvaColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HsvaColor>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HsvaBase extends AlphaColorPicker<HsvaColor> {
    protected get colorModel(): ColorModel<HsvaColor>;
}
//# sourceMappingURL=hsva.d.ts.map