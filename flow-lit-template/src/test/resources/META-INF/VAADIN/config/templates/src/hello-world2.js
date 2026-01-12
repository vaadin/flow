// Import an element
import { LitElement, html } from 'lit';

// Define an element class
export class HelloWorld extends LitElement {
  // Define the element's template
  render() {
    return html`
      <style>
        :host {
          margin: 5px;
        }

        .response {
          margin-top: 10px;
        }
      </style>
      <div>Tag name doesn't match the JS module name<div>inner</div></div>
      <div id="test" class="response">Web components like you, too.</div>
    `;
  }
}

// Register the element with the browser
customElements.define('hello-world-lit', HelloWorld);
