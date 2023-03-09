import type { ColorModel, ColorPickerEventListener, ColorPickerEventMap, HsvColor } from '../types';
import { ColorPicker } from '../components/color-picker.js';
export interface HsvBase {
    addEventListener<T extends keyof ColorPickerEventMap<HsvColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HsvColor>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<HsvColor>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<HsvColor>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HsvBase extends ColorPicker<HsvColor> {
    protected get colorModel(): ColorModel<HsvColor>;
}
//# sourceMappingURL=hsv.d.ts.map