import { expect, fixture, html } from '@open-wc/testing';
import { detectTheme } from './detector';
import { testElementMetadata } from './tests/utils';

describe('theme-detector', () => {
  it('should include all CSS property values from component metadata', () => {
    const theme = detectTheme(testElementMetadata);
    let propertyCount = 0;

    testElementMetadata.elements.forEach((element) => {
      element.properties.forEach((property) => {
        propertyCount++;
        const propertyValue = theme.getPropertyValue(element.selector, property.propertyName);
        expect(propertyValue).to.exist;
      });
    });

    expect(theme.properties.length).to.equal(propertyCount);
  });

  it('should detect default CSS property values', async () => {
    const theme = detectTheme(testElementMetadata);

    expect(theme.getPropertyValue('test-element', 'padding').value).to.equal('10px');
    expect(theme.getPropertyValue('test-element::part(label)', 'color').value).to.equal('rgb(0, 0, 0)');
    expect(theme.getPropertyValue('test-element input[slot="input"]', 'border-radius').value).to.equal('5px');
    expect(theme.getPropertyValue('test-element::part(helper-text)', 'color').value).to.equal('rgb(200, 200, 200)');
  });

  it('should detect themed CSS property values', async () => {
    await fixture(html`
      <style>
        test-element {
          padding: 20px;
        }

        test-element::part(label) {
          color: green;
        }

        test-element input[slot='input'] {
          border-radius: 10px;
        }

        test-element::part(helper-text) {
          color: blue;
        }
      </style>
    `);
    const theme = detectTheme(testElementMetadata);

    expect(theme.getPropertyValue('test-element', 'padding').value).to.equal('20px');
    expect(theme.getPropertyValue('test-element::part(label)', 'color').value).to.equal('rgb(0, 128, 0)');
    expect(theme.getPropertyValue('test-element input[slot="input"]', 'border-radius').value).to.equal('10px');
    expect(theme.getPropertyValue('test-element::part(helper-text)', 'color').value).to.equal('rgb(0, 0, 255)');
  });

  it('should remove test component from DOM', () => {
    detectTheme(testElementMetadata);

    expect(document.querySelector('test-element')).to.not.exist;
  });
});
