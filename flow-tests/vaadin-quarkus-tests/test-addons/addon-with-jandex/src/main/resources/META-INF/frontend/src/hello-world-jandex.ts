import { html, LitElement } from 'lit';
import '@axa-ch/input-text';

class HelloWorldJandex extends LitElement {

    render() {
        return html`
            <div>
                <span id="with-jandex">Addon with Jandex index</span>
                <axa-input-text id="npm-dep-jandex"></axa-input-text>
            </div>`;
    }
}

customElements.define('hello-world-jandex', HelloWorldJandex);
