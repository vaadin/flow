import { expect, fixture, html } from '@open-wc/testing';
import { themePreview } from './preview';
import { ComponentTheme, Theme } from './model';
import { testElementMetadata } from './tests/utils';

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
    const theme = new Theme();
    const componentTheme = new ComponentTheme(testElementMetadata);
    componentTheme.updatePropertyValue(null, 'padding', '20px');
    componentTheme.updatePropertyValue('label', 'color', 'red');
    theme.updateComponentTheme(componentTheme);
    themePreview.update(theme);

    const elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('20px');
    expect(elementStyles.label.color).to.equal('rgb(255, 0, 0)');
  });

  it('should update theme preview', () => {
    const theme = new Theme();
    const componentTheme = new ComponentTheme(testElementMetadata);
    componentTheme.updatePropertyValue(null, 'padding', '20px');
    componentTheme.updatePropertyValue('label', 'color', 'red');
    theme.updateComponentTheme(componentTheme);
    themePreview.update(theme);

    componentTheme.updatePropertyValue(null, 'padding', '30px');
    componentTheme.updatePropertyValue('label', 'color', 'green');
    theme.updateComponentTheme(componentTheme);
    themePreview.update(theme);

    const elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('30px');
    expect(elementStyles.label.color).to.equal('rgb(0, 128, 0)');
  });

  it('should reset theme preview', () => {
    const theme = new Theme();
    const componentTheme = new ComponentTheme(testElementMetadata);
    componentTheme.updatePropertyValue(null, 'padding', '20px');
    componentTheme.updatePropertyValue('label', 'color', 'red');
    theme.updateComponentTheme(componentTheme);
    themePreview.update(theme);

    themePreview.reset();

    const elementStyles = getElementStyles();
    expect(elementStyles.host.padding).to.equal('10px');
    expect(elementStyles.label.color).to.equal('rgb(0, 0, 0)');
  });
});
