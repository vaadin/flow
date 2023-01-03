import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('dnd-palette')
export class DndPalette extends LitElement {
  static globalStyles = `
    dnd-palette {
      position: fixed;
      top: 0;
      left: 0;
      background: white;
      border: 1px solid black;
      z-index: 100;
      padding: 1em;
      background-color: rgba(45, 45, 45, 0.98);
      padding: 0.1875rem 0.75rem 0.1875rem 1rem;
      border-radius: 0.5rem;
      margin: 0.5rem;
      box-shadow: var(--dev-tools-box-shadow);
      color: rgba(255, 255, 255, 0.8);
    }

    dnd-palette  h4 {
        margin-top: 0;
        color: rgba(255, 255, 255, 0.8);

    }
    [palette] {
        border: 1px solid grey;
        padding: 0.5em;
        display: inline-block;    }
  `;

  connectedCallback() {
    super.connectedCallback();
    const styleSheet = new CSSStyleSheet();
    styleSheet.replaceSync(DndPalette.globalStyles);
    document.adoptedStyleSheets.push(styleSheet);
  }
  render() {
    return html`
      <h4>Add a new component</h4>
      <div palette draggable="true">
        Button
        <template><vaadin-button>A button</vaadin-button></template>
      </div>
      <div palette draggable="true">
        TextField
        <template><vaadin-text-field label="TextField"></vaadin-text-field></template>
      </div>
    `;
  }

  createRenderRoot() {
    return this;
  }
}
