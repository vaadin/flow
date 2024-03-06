/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
// Import an element
import { LitElement, html } from 'lit';

// Define an element class
export class MyGreedyLitElement extends LitElement {

  // Define the element's template
  render() {
    return html`
      <style>
        :host{ 
          margin: 5px; 
        }
      
        .response { margin-top: 10px; } 
      </style>
   <div>\`Tag name doesn't match the JS module name<div>inner</div></div>   <div id='test'  class='response'>greedy</div>
    `;}
  static get styles() { return css`:host { background-color: pink } <span>incorrect content</span>`;  }
}

// Register the element with the browser
customElements.define('my-greedy-element', MyGreedyLitElement);
