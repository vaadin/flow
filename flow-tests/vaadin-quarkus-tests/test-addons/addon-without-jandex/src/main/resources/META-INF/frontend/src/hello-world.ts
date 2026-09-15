import { html, LitElement } from 'lit';
import '@axa-ch/input-text';

class HelloWorld extends LitElement {

    render() {
        return html`
            <div>
                <span id="without-jandex">Addon without Jandex index</span>
                <axa-input-text id="npm-dep"></axa-input-text>
            </div>`;
    }
}

customElements.define('hello-world', HelloWorld);
