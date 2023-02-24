import { expect, fixture, html } from '@open-wc/testing';
import '@vaadin/button';
import buttonMetadata from './metadata/components/vaadin-button';
import { themePreview } from './preview';
import { ComponentTheme } from './model';

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

    const theme = new ComponentTheme(buttonMetadata);
    theme.updatePropertyValue('label', 'color', colors.red);
    theme.updatePropertyValue('prefix', 'color', colors.green);
    theme.updatePropertyValue('suffix', 'color', colors.blue);
    themePreview.update(theme);

    buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.equal(colors.red);
    expect(buttonStyles.prefix.color).to.equal(colors.green);
    expect(buttonStyles.suffix.color).to.equal(colors.blue);
  });

  it('should update theme preview', () => {
    const theme = new ComponentTheme(buttonMetadata);
    theme.updatePropertyValue('label', 'color', colors.red);
    theme.updatePropertyValue('prefix', 'color', colors.green);
    theme.updatePropertyValue('suffix', 'color', colors.blue);
    themePreview.update(theme);

    theme.updatePropertyValue('label', 'color', colors.red);
    theme.updatePropertyValue('prefix', 'color', colors.red);
    theme.updatePropertyValue('suffix', 'color', colors.red);
    themePreview.update(theme);

    let buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.equal(colors.red);
    expect(buttonStyles.prefix.color).to.equal(colors.red);
    expect(buttonStyles.suffix.color).to.equal(colors.red);
  });

  it('should clear theme preview', () => {
    const theme = new ComponentTheme(buttonMetadata);
    theme.updatePropertyValue('label', 'color', colors.red);
    theme.updatePropertyValue('prefix', 'color', colors.green);
    theme.updatePropertyValue('suffix', 'color', colors.blue);
    themePreview.update(theme);

    themePreview.update(new ComponentTheme(buttonMetadata));

    let buttonStyles = getButtonStyles();
    expect(buttonStyles.label.color).to.not.equal(colors.red);
    expect(buttonStyles.suffix.color).to.not.equal(colors.green);
    expect(buttonStyles.prefix.color).to.not.equal(colors.blue);
  });
});
