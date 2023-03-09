import type { ColorPickerEventListener, ColorPickerEventMap } from '../types';
declare const $alpha: unique symbol;
declare const $color: unique symbol;
declare const $saved: unique symbol;
declare const $input: unique symbol;
declare const $init: unique symbol;
declare const $prefix: unique symbol;
declare const $update: unique symbol;
export interface HexInputBase {
    addEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | AddEventListenerOptions): void;
    removeEventListener<T extends keyof ColorPickerEventMap<string>>(type: T, listener: ColorPickerEventListener<ColorPickerEventMap<string>[T]>, options?: boolean | EventListenerOptions): void;
}
export declare class HexInputBase extends HTMLElement {
    static get observedAttributes(): string[];
    private [$color];
    private [$alpha];
    private [$prefix];
    private [$saved];
    private [$input];
    get color(): string;
    set color(hex: string);
    get alpha(): boolean;
    set alpha(alpha: boolean);
    get prefixed(): boolean;
    set prefixed(prefixed: boolean);
    constructor();
    connectedCallback(): void;
    handleEvent(event: Event): void;
    attributeChangedCallback(attr: string, _oldVal: string, newVal: string): void;
    private [$init];
    private [$update];
}
export {};
//# sourceMappingURL=hex-input.d.ts.map