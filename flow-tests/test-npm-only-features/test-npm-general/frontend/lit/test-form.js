import {html, LitElement} from 'lit-element';

class TestForm extends LitElement {

  render() {
    return html`
        <div id="div">Template text</div> 
    `;
  }
}

customElements.define('test-form', TestForm);
