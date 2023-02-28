import { ComponentMetadata } from '../metadata/model';

export class TestElement extends HTMLElement {
  constructor() {
    super();

    const shadow = this.attachShadow({ mode: 'open' });
    const label = document.createElement('div');
    label.setAttribute('part', 'label');
    shadow.append(label);

    const styles = new CSSStyleSheet();
    styles.replaceSync(`
      :host {
        padding: 10px;
      }

      [part='label'] {
        color: black;
      }
    `);
    shadow.adoptedStyleSheets.push(styles);
  }
}
customElements.define('test-element', TestElement);

export const testElementMetadata: ComponentMetadata = {
  tagName: 'test-element',
  displayName: 'Test element',
  properties: [
    {
      propertyName: 'padding',
      displayName: 'Padding'
    }
  ],
  parts: [
    {
      partName: 'label',
      displayName: 'Label',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color'
        }
      ]
    }
  ]
};
