import { Slider, Offset } from './slider.js';
import type { HsvaColor } from '../types';
export declare class Hue extends Slider {
    h: number;
    constructor(root: ShadowRoot);
    update({ h }: HsvaColor): void;
    getMove(offset: Offset, key?: boolean): Record<string, number>;
}
//# sourceMappingURL=hue.d.ts.map