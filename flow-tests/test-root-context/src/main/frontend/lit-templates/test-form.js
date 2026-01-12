import { html, LitElement } from 'lit';

class TestForm extends LitElement {
  render() {
    return html` <div id="div">Template text</div> `;
  }
}

customElements.define('test-form', TestForm);
