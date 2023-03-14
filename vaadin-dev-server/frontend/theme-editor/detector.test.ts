import { expect, fixture, html } from '@open-wc/testing';
import { detectTheme } from './detector';
import { testElementMetadata } from './tests/utils';

describe('theme-detector', () => {
  it('should include all CSS property values from component metadata', () => {
    const theme = detectTheme(testElementMetadata);
    let propertyCount = 0;

    // Host properties
    testElementMetadata.properties.forEach((property) => {
      propertyCount++;
      const propertyValue = theme.getPropertyValue(null, property.propertyName);
      expect(propertyValue).to.exist;
    });
    // Part properties
    testElementMetadata.parts.forEach((part) => {
      part.properties.forEach((property) => {
        propertyCount++;
        const propertyValue = theme.getPropertyValue(part.partName, property.propertyName);
        expect(propertyValue).to.exist;
      });
    });

    expect(theme.properties.length).to.equal(propertyCount);
  });

  it('should detect default CSS property values', async () => {
    const theme = detectTheme(testElementMetadata);

    expect(theme.getPropertyValue(null, 'padding').value).to.equal('10px');
    expect(theme.getPropertyValue('label', 'color').value).to.equal('rgb(0, 0, 0)');
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
      </style>
    `);
    const theme = detectTheme(testElementMetadata);

    expect(theme.getPropertyValue(null, 'padding').value).to.equal('20px');
    expect(theme.getPropertyValue('label', 'color').value).to.equal('rgb(0, 128, 0)');
  });

  it('should remove test component from DOM', () => {
    detectTheme(testElementMetadata);

    expect(document.querySelector('test-element')).to.not.exist;
  });
});
