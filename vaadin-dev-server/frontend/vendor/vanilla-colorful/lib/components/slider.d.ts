import type { HsvaColor } from '../types.js';
export interface Offset {
    x: number;
    y: number;
}
export declare abstract class Slider {
    nodes: HTMLElement[];
    el: HTMLElement;
    xy: boolean;
    constructor(root: ShadowRoot, part: string, aria: string, xy: boolean);
    set dragging(state: boolean);
    handleEvent(event: Event): void;
    abstract getMove(offset: Offset, key?: boolean): Record<string, number>;
    abstract update(hsva: HsvaColor): void;
    style(styles: Array<Record<string, string>>): void;
}
//# sourceMappingURL=slider.d.ts.map