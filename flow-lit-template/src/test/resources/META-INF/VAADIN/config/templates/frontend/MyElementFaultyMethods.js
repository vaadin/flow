// Import an element
import { LitElement, html } from 'lit';

// Define an element class
export class MyLitElement extends LitElement {
  // Define public API properties
  // Define the element's template
  render() {
    return `
      <style>
        :host{ 
          margin: 5px; 
        }
      
        .response { margin-top: 10px; } 
      </style>
        <div id="test"  class="response">Web components like you, too.</div>
    `;
  }
}

// Register the element with the browser
customElements.define('my-element', MyLitElement);
