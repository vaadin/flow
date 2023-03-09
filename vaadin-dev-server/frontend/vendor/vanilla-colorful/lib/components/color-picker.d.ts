import type { AnyColor, ColorModel } from '../types';
import type { Slider } from './slider.js';
declare const $isSame: unique symbol;
declare const $color: unique symbol;
declare const $hsva: unique symbol;
declare const $update: unique symbol;
declare const $parts: unique symbol;
export declare const $css: unique symbol;
export declare const $sliders: unique symbol;
export declare type Sliders = Array<new (root: ShadowRoot) => Slider>;
export declare abstract class ColorPicker<C extends AnyColor> extends HTMLElement {
    static get observedAttributes(): string[];
    protected get [$css](): string[];
    protected get [$sliders](): Sliders;
    protected abstract get colorModel(): ColorModel<C>;
    private [$hsva];
    private [$color];
    private [$parts];
    get color(): C;
    set color(newColor: C);
    constructor();
    connectedCallback(): void;
    attributeChangedCallback(_attr: string, _oldVal: string, newVal: string): void;
    handleEvent(event: CustomEvent): void;
    private [$isSame];
    private [$update];
}
export {};
//# sourceMappingURL=color-picker.d.ts.map