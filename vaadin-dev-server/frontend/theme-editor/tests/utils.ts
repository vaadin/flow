import { ComponentMetadata } from '../metadata/model';

export class TestElement extends HTMLElement {
  constructor() {
    super();

    const shadow = this.attachShadow({ mode: 'open' });
    const label = document.createElement('div');
    label.setAttribute('part', 'label');
    shadow.append(label);

    const inputSlot = document.createElement('slot');
    inputSlot.setAttribute('name', 'input');
    shadow.append(inputSlot);

    const helperText = document.createElement('div');
    helperText.setAttribute('part', 'helper-text');
    shadow.append(helperText);

    const styles = new CSSStyleSheet();
    styles.replaceSync(`
      :host {
        padding: 10px;
      }

      [part='label'] {
        color: black;
      }
      
      ::slotted(input) {
        border: solid 1px black;
        border-radius: 5px;
      }

      [part='helper-text'] {
        color: rgb(200, 200, 200);
      }
    `);
    shadow.adoptedStyleSheets.push(styles);
  }

  connectedCallback() {
    const input = document.createElement('input');
    input.setAttribute('slot', 'input');
    this.append(input);
  }
}

customElements.define('test-element', TestElement);

export const testElementMetadata: ComponentMetadata = {
  tagName: 'test-element',
  displayName: 'Test element',
  elements: [
    {
      selector: 'test-element',
      displayName: 'Host',
      properties: [
        {
          propertyName: 'padding',
          displayName: 'Padding'
        },
        {
          propertyName: 'background',
          displayName: 'Background'
        },
        {
          propertyName: '--custom-property',
          displayName: 'Custom property',
          defaultValue: 'foo'
        }
      ]
    },
    {
      selector: 'test-element::part(label)',
      displayName: 'Label',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color'
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size'
        }
      ]
    },
    {
      selector: 'test-element input[slot="input"]',
      displayName: 'Input',
      properties: [
        {
          propertyName: 'border-radius',
          displayName: 'Border radius'
        }
      ]
    },
    {
      selector: 'test-element::part(helper-text)',
      displayName: 'Helper text',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color'
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size'
        }
      ]
    }
  ],
  setupElement() {
    return Promise.resolve();
  },
  cleanupElement() {
    return Promise.resolve();
  }
};
