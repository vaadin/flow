import { expect, fixture, html } from '@open-wc/testing';
import '@vaadin/button';
import buttonMetadata from './metadata/components/vaadin-button';
import { themePreview } from './preview';
import {ComponentTheme, Theme} from './model';

describe('theme-preview', () => {
  const colors = {
    red: 'rgb(255, 0, 0)',
    green: 'rgb(0, 255, 0)',
    blue: 'rgb(0, 0, 255)'
  };

  let button: HTMLElement;

  function getButtonStyles() {
    return {
      label: getComputedStyle(button.shadowRoot!.querySelector('[part="label"]')!),
      prefix: getComputedStyle(button.shadowRoot!.querySelector('[part="prefix"]')!),
      suffix: getComputedStyle(button.shadowRoot!.querySelector('[part="suffix"]')!)
    };
  }

  beforeEach(async () => {
    themePreview.stylesheet.replaceSync('');
    button = await fixture(html` <vaadin-button></vaadin-button>`);
  });

  it('should apply theme preview', () => {
    let buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.not.equal(colors.red);
    expect(buttonStyles.suffix.color).to.not.equal(colors.green);
    expect(buttonStyles.prefix.color).to.not.equal(colors.blue);

    const theme = new Theme();
    const buttonTheme = new ComponentTheme(buttonMetadata);
    buttonTheme.updatePropertyValue('label', 'color', colors.red);
    buttonTheme.updatePropertyValue('prefix', 'color', colors.green);
    buttonTheme.updatePropertyValue('suffix', 'color', colors.blue);
    theme.updateComponentTheme(buttonTheme);
    themePreview.update(theme);

    buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.equal(colors.red);
    expect(buttonStyles.prefix.color).to.equal(colors.green);
    expect(buttonStyles.suffix.color).to.equal(colors.blue);
  });

  it('should update theme preview', () => {
    const theme = new Theme();
    const buttonTheme = new ComponentTheme(buttonMetadata);
    buttonTheme.updatePropertyValue('label', 'color', colors.red);
    buttonTheme.updatePropertyValue('prefix', 'color', colors.green);
    buttonTheme.updatePropertyValue('suffix', 'color', colors.blue);
    theme.updateComponentTheme(buttonTheme);
    themePreview.update(theme);

    buttonTheme.updatePropertyValue('label', 'color', colors.red);
    buttonTheme.updatePropertyValue('prefix', 'color', colors.red);
    buttonTheme.updatePropertyValue('suffix', 'color', colors.red);
    theme.updateComponentTheme(buttonTheme);
    themePreview.update(theme);

    let buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.equal(colors.red);
    expect(buttonStyles.prefix.color).to.equal(colors.red);
    expect(buttonStyles.suffix.color).to.equal(colors.red);
  });

  it('should reset theme preview', () => {
    const theme = new Theme();
    const buttonTheme = new ComponentTheme(buttonMetadata);
    buttonTheme.updatePropertyValue('label', 'color', colors.red);
    buttonTheme.updatePropertyValue('prefix', 'color', colors.green);
    buttonTheme.updatePropertyValue('suffix', 'color', colors.blue);
    theme.updateComponentTheme(buttonTheme)
    themePreview.update(theme);

    themePreview.reset();

    let buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.not.equal(colors.red);
    expect(buttonStyles.suffix.color).to.not.equal(colors.green);
    expect(buttonStyles.prefix.color).to.not.equal(colors.blue);
  });
});
