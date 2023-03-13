import { validHex } from '../utils/validate.js';
import { tpl } from '../utils/dom.js';
const template = tpl('<slot><input part="input" spellcheck="false"></slot>');
// Escapes all non-hexadecimal characters including "#"
const escape = (hex, alpha) => hex.replace(/([^0-9A-F]+)/gi, '').substring(0, alpha ? 8 : 6);
const $alpha = Symbol('alpha');
const $color = Symbol('color');
const $saved = Symbol('saved');
const $input = Symbol('input');
const $init = Symbol('init');
const $prefix = Symbol('prefix');
const $update = Symbol('update');
export class HexInputBase extends HTMLElement {
    static get observedAttributes() {
        return ['alpha', 'color', 'prefixed'];
    }
    get color() {
        return this[$color];
    }
    set color(hex) {
        this[$color] = hex;
        this[$update](hex);
    }
    get alpha() {
        return this[$alpha];
    }
    set alpha(alpha) {
        this[$alpha] = alpha;
        this.toggleAttribute('alpha', alpha);
        // When alpha set to false, update color
        const color = this.color;
        if (color && !validHex(color, alpha)) {
            this.color = color.startsWith('#')
                ? color.substring(0, color.length === 5 ? 4 : 7)
                : color.substring(0, color.length === 4 ? 3 : 6);
        }
    }
    get prefixed() {
        return this[$prefix];
    }
    set prefixed(prefixed) {
        this[$prefix] = prefixed;
        this.toggleAttribute('prefixed', prefixed);
        this[$update](this.color);
    }
    constructor() {
        super();
        const root = this.attachShadow({ mode: 'open' });
        root.appendChild(template.content.cloneNode(true));
        const slot = root.firstElementChild;
        slot.addEventListener('slotchange', () => this[$init](root));
    }
    connectedCallback() {
        this[$init](this.shadowRoot);
        // A user may set a property on an _instance_ of an element,
        // before its prototype has been connected to this class.
        // If so, we need to run it through the proper class setter.
        if (this.hasOwnProperty('alpha')) {
            const value = this.alpha;
            delete this['alpha'];
            this.alpha = value;
        }
        else {
            this.alpha = this.hasAttribute('alpha');
        }
        if (this.hasOwnProperty('prefixed')) {
            const value = this.prefixed;
            delete this['prefixed'];
            this.prefixed = value;
        }
        else {
            this.prefixed = this.hasAttribute('prefixed');
        }
        if (this.hasOwnProperty('color')) {
            const value = this.color;
            delete this['color'];
            this.color = value;
        }
        else if (this.color == null) {
            this.color = this.getAttribute('color') || '';
        }
        else if (this[$color]) {
            this[$update](this[$color]);
        }
    }
    handleEvent(event) {
        const target = event.target;
        const { value } = target;
        switch (event.type) {
            case 'input':
                const hex = escape(value, this.alpha);
                this[$saved] = this.color;
                if (validHex(hex, this.alpha) || value === '') {
                    this.color = hex;
                    this.dispatchEvent(new CustomEvent('color-changed', {
                        bubbles: true,
                        detail: { value: hex ? '#' + hex : '' }
                    }));
                }
                break;
            case 'blur':
                if (value && !validHex(value, this.alpha)) {
                    this.color = this[$saved];
                }
        }
    }
    attributeChangedCallback(attr, _oldVal, newVal) {
        if (attr === 'color' && this.color !== newVal) {
            this.color = newVal;
        }
        const hasBooleanAttr = newVal != null;
        if (attr === 'alpha') {
            if (this.alpha !== hasBooleanAttr) {
                this.alpha = hasBooleanAttr;
            }
        }
        if (attr === 'prefixed') {
            if (this.prefixed !== hasBooleanAttr) {
                this.prefixed = hasBooleanAttr;
            }
        }
    }
    [$init](root) {
        let input = this.querySelector('input');
        if (!input) {
            // remove all child node if no input found
            let c;
            while ((c = this.firstChild)) {
                c.remove();
            }
            input = root.querySelector('input');
        }
        input.addEventListener('input', this);
        input.addEventListener('blur', this);
        this[$input] = input;
        this[$update](this.color);
    }
    [$update](hex) {
        if (this[$input]) {
            this[$input].value =
                hex == null || hex == '' ? '' : (this.prefixed ? '#' : '') + escape(hex, this.alpha);
        }
    }
}
//# sourceMappingURL=hex-input.js.map