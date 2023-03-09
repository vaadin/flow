import { Slider, Offset } from './slider.js';
import type { HsvaColor } from '../types';
export declare class Alpha extends Slider {
    hsva: HsvaColor;
    constructor(root: ShadowRoot);
    update(hsva: HsvaColor): void;
    getMove(offset: Offset, key?: boolean): Record<string, number>;
}
//# sourceMappingURL=alpha.d.ts.map