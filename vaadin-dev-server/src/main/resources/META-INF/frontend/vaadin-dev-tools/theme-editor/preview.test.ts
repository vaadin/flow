import { expect, fixture, html } from '@open-wc/testing';
import { themePreview } from './preview';
import './tests/utils';

describe('theme-preview', () => {
  let element: HTMLElement;

  function getElementStyles() {
    return {
      host: getComputedStyle(element),
      label: getComputedStyle(element.shadowRoot!.querySelector('[part="label"]')!)
    };
  }

  beforeEach(async () => {
    themePreview.stylesheet.replaceSync('');
    element = await fixture(html` <test-element></test-element>`);
  });

  it('should apply theme preview', () => {
    const css = `
      test-element {
        padding: 20px;
      }
      
      test-element::part(label) {
        color: red;
      }
    `;
    themePreview.update(css);

    const elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('20px');
    expect(elementStyles.label.color).to.equal('rgb(255, 0, 0)');
  });

  it('should update theme preview', () => {
    const css = `
      test-element {
        padding: 20px;
      }
      
      test-element::part(label) {
        color: red;
      }
    `;
    themePreview.update(css);

    const updatedCss = `
      test-element {
        padding: 30px;
      }
      
      test-element::part(label) {
        color: green;
      }
    `;
    themePreview.update(updatedCss);

    const elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('30px');
    expect(elementStyles.label.color).to.equal('rgb(0, 128, 0)');
  });

  it('should reset theme preview', () => {
    const css = `
      test-element {
        padding: 20px;
      }
      
      test-element::part(label) {
        color: red;
      }
    `;
    themePreview.update(css);
    themePreview.update('');

    const elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('10px');
    expect(elementStyles.label.color).to.equal('rgb(0, 0, 0)');
  });
});
