import { expect, fixture, html } from '@open-wc/testing';
import '@vaadin/button';
import buttonMetadata from './metadata/components/vaadin-button';
import { detectTheme } from './detector';

describe('theme-detector', () => {
  it('should include all CSS property values from component metadata', () => {
    const theme = detectTheme(buttonMetadata);
    let propertyCount = 0;

    buttonMetadata.parts.forEach((part) => {
      part.properties.forEach((property) => {
        propertyCount++;

        const propertyValue = theme.getPropertyValue(part.partName, property.propertyName);
        expect(propertyValue).to.exist;
      });
    });

    expect(theme.properties.length).to.equal(propertyCount);
  });

  it('should detect default CSS property values', async () => {
    const theme = detectTheme(buttonMetadata);

    const button = await fixture(html` <vaadin-button></vaadin-button>`);
    const labelColor = getComputedStyle(button.shadowRoot!.querySelector('[part="label"]')!).color;
    const prefixColor = getComputedStyle(button.shadowRoot!.querySelector('[part="prefix"]')!).color;
    const suffixColor = getComputedStyle(button.shadowRoot!.querySelector('[part="suffix"]')!).color;

    expect(theme.getPropertyValue('label', 'color').value).to.equal(labelColor);
    expect(theme.getPropertyValue('prefix', 'color').value).to.equal(prefixColor);
    expect(theme.getPropertyValue('suffix', 'color').value).to.equal(suffixColor);
  });

  it('should detect themed CSS property values', async () => {
    await fixture(html`
      <style>
        vaadin-button::part(label) {
          color: red;
        }

        vaadin-button::part(prefix) {
          color: rgb(0, 255, 0);
        }

        vaadin-button::part(suffix) {
          color: blue;
        }
      </style>
    `);
    const theme = detectTheme(buttonMetadata);

    expect(theme.getPropertyValue('label', 'color').value).to.equal('rgb(255, 0, 0)');
    expect(theme.getPropertyValue('prefix', 'color').value).to.equal('rgb(0, 255, 0)');
    expect(theme.getPropertyValue('suffix', 'color').value).to.equal('rgb(0, 0, 255)');
  });

  it('should remove test component from DOM', () => {
    detectTheme(buttonMetadata);

    expect(document.querySelector('vaadin-button')).to.not.exist;
  });
});
