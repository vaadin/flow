import { equalColorObjects } from '../utils/compare.js';
import { fire, tpl } from '../utils/dom.js';
import { Hue } from './hue.js';
import { Saturation } from './saturation.js';
import css from '../styles/color-picker.js';
import hueCss from '../styles/hue.js';
import saturationCss from '../styles/saturation.js';
const $isSame = Symbol('same');
const $color = Symbol('color');
const $hsva = Symbol('hsva');
const $update = Symbol('update');
const $parts = Symbol('parts');
export const $css = Symbol('css');
export const $sliders = Symbol('sliders');
export class ColorPicker extends HTMLElement {
    static get observedAttributes() {
        return ['color'];
    }
    get [$css]() {
        return [css, hueCss, saturationCss];
    }
    get [$sliders]() {
        return [Saturation, Hue];
    }
    get color() {
        return this[$color];
    }
    set color(newColor) {
        if (!this[$isSame](newColor)) {
            const newHsva = this.colorModel.toHsva(newColor);
            this[$update](newHsva);
            this[$color] = newColor;
        }
    }
    constructor() {
        super();
        const template = tpl(`<style>${this[$css].join('')}</style>`);
        const root = this.attachShadow({ mode: 'open' });
        root.appendChild(template.content.cloneNode(true));
        root.addEventListener('move', this);
        this[$parts] = this[$sliders].map((slider) => new slider(root));
    }
    connectedCallback() {
        // A user may set a property on an _instance_ of an element,
        // before its prototype has been connected to this class.
        // If so, we need to run it through the proper class setter.
        if (this.hasOwnProperty('color')) {
            const value = this.color;
            delete this['color'];
            this.color = value;
        }
        else if (!this.color) {
            this.color = this.colorModel.defaultColor;
        }
    }
    attributeChangedCallback(_attr, _oldVal, newVal) {
        const color = this.colorModel.fromAttr(newVal);
        if (!this[$isSame](color)) {
            this.color = color;
        }
    }
    handleEvent(event) {
        // Merge the current HSV color object with updated params.
        const oldHsva = this[$hsva];
        const newHsva = { ...oldHsva, ...event.detail };
        this[$update](newHsva);
        let newColor;
        if (!equalColorObjects(newHsva, oldHsva) &&
            !this[$isSame]((newColor = this.colorModel.fromHsva(newHsva)))) {
            this[$color] = newColor;
            fire(this, 'color-changed', { value: newColor });
        }
    }
    [$isSame](color) {
        return this.color && this.colorModel.equal(color, this.color);
    }
    [$update](hsva) {
        this[$hsva] = hsva;
        this[$parts].forEach((part) => part.update(hsva));
    }
}
//# sourceMappingURL=color-picker.js.map