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

  it('should add CSS to preview stylesheet', () => {
    const css = `
      test-element {
        padding: 20px;
      }
      
      test-element::part(label) {
        color: red;
      }
    `;
    themePreview.add(css);

    let elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('20px');
    expect(elementStyles.label.color).to.equal('rgb(255, 0, 0)');

    const updatedCss = `
      test-element {
        padding: 30px;
      }
      
      test-element::part(label) {
        color: green;
      }
    `;
    themePreview.add(updatedCss);

    elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('30px');
    expect(elementStyles.label.color).to.equal('rgb(0, 128, 0)');
  });

  it('should clear preview stylesheet', () => {
    const css = `
      test-element {
        padding: 20px;
      }
      
      test-element::part(label) {
        color: red;
      }
    `;
    themePreview.add(css);
    let elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('20px');
    expect(elementStyles.label.color).to.equal('rgb(255, 0, 0)');

    themePreview.clear();
    elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('10px');
    expect(elementStyles.label.color).to.equal('rgb(0, 0, 0)');
  });

  describe('preview local class name', () => {
    let element: HTMLElement;

    beforeEach(async () => {
      element = await fixture(html` <test-element></test-element>`);
      // Reset applied class names
      (themePreview as any)._localClassNameMap = new Map();
    });

    it('should apply class name to element', () => {
      themePreview.previewLocalClassName(element, 'test-class');

      expect(element.classList.contains('test-class')).to.be.true;
    });

    it('should update class name on element', () => {
      themePreview.previewLocalClassName(element, 'test-class');
      themePreview.previewLocalClassName(element, 'other-class');

      expect(element.classList.contains('test-class')).to.be.false;
      expect(element.classList.contains('other-class')).to.be.true;
    });

    it('should remove class name from element', () => {
      themePreview.previewLocalClassName(element, 'test-class');
      themePreview.previewLocalClassName(element);

      expect(element.classList.contains('test-class')).to.be.false;
      expect(element.classList.length).to.equal(0);
    });

    it('should not add undefined class name', () => {
      themePreview.previewLocalClassName(element);

      expect(element.classList.length).to.equal(0);
    });
  });
});
