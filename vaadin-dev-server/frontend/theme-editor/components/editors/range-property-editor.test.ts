import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import sinon from 'sinon';
import { ComponentElementMetadata, CssPropertyMetadata } from '../../metadata/model';
import { ComponentTheme } from '../../model';
import { testElementMetadata } from '../../tests/utils';
import { RangePropertyEditor } from './range-property-editor';
import './range-property-editor';

const heightMetadata: CssPropertyMetadata = {
  propertyName: 'height',
  displayName: 'Height',
  presets: ['--test-size-xs', '--test-size-s', '--test-size-m', '--test-size-l', '--test-size-xl']
};
const paddingMetadata: CssPropertyMetadata = {
  propertyName: 'padding',
  displayName: 'Padding',
  presets: ['--test-space-s', '--test-space-m', '--test-space-l']
};
const hostMetadata: ComponentElementMetadata = {
  selector: 'test-element',
  displayName: 'Host',
  properties: [heightMetadata, paddingMetadata]
};

describe('range property editor', () => {
  let theme: ComponentTheme;
  let editor: RangePropertyEditor;
  let valueChangeSpy: sinon.SinonSpy;

  beforeEach(async () => {
    // Define custom CSS properties
    await fixture(html` <style>
      html {
        --test-size-xs: 20px;
        --test-size-s: 30px;
        --test-size-m: 40px;
        --test-size-l: 50px;
        --test-size-xl: 60px;

        --test-space-s: 5px;
        --test-space-m: 15px;
        --test-space-l: 25px;
      }
    </style>`);

    theme = new ComponentTheme(testElementMetadata);
    theme.updatePropertyValue(hostMetadata.selector, 'height', '40px');
    theme.updatePropertyValue(hostMetadata.selector, 'padding', '15px');
    valueChangeSpy = sinon.spy();

    editor = await fixture(html` <vaadin-dev-tools-theme-range-property-editor
      .theme=${theme}
      .elementMetadata=${hostMetadata}
      .propertyMetadata=${heightMetadata}
      @theme-property-value-change=${valueChangeSpy}
    >
    </vaadin-dev-tools-theme-range-property-editor>`);
  });

  function getSlider() {
    return editor.shadowRoot!.querySelector('input[type="range"]') as HTMLInputElement;
  }

  function getSliderWrapper() {
    return editor.shadowRoot!.querySelector('.slider-wrapper') as HTMLInputElement;
  }

  function getInput() {
    return editor
      .shadowRoot!.querySelector('vaadin-dev-tools-theme-text-input')!
      .shadowRoot!.querySelector('input') as HTMLInputElement;
  }

  function getClearButton() {
    return editor
      .shadowRoot!.querySelector('vaadin-dev-tools-theme-text-input')!
      .shadowRoot!.querySelector('button') as HTMLButtonElement;
  }

  function isClearButtonVisible() {
    const button = getClearButton();

    return getComputedStyle(button).display === 'block';
  }

  function cloneTheme() {
    const result = new ComponentTheme(testElementMetadata);
    result.addPropertyValues(theme.properties);
    return result;
  }

  it('should display correct number of presets in slider', () => {
    const slider = getSlider();
    const styles = getComputedStyle(slider);

    expect(slider.getAttribute('min')).to.equal('0');
    expect(slider.getAttribute('max')).to.equal('4');
    expect(slider.getAttribute('step')).to.equal('1');
    expect(styles.getPropertyValue('--preset-count').trim()).to.equal('5');
  });

  it('should update slider presets when changing property metadata', async () => {
    editor.propertyMetadata = paddingMetadata;
    await elementUpdated(editor);

    const slider = getSlider();
    const styles = getComputedStyle(slider);

    expect(slider.getAttribute('min')).to.equal('0');
    expect(slider.getAttribute('max')).to.equal('2');
    expect(slider.getAttribute('step')).to.equal('1');
    expect(styles.getPropertyValue('--preset-count').trim()).to.equal('3');
  });

  it('should select slider preset if theme value matches raw preset value', () => {
    expect(getSlider().value).to.equal('2');
    expect(Array.from(getSliderWrapper().classList)).to.not.contain('custom-value');
  });

  it('should should mark slider as having custom value if theme value does not match a preset value', async () => {
    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(hostMetadata.selector, 'height', '25px');
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    // Internal model value is -1, but browsers represent invalid value as 0 instead
    expect(getSlider().value).to.equal('0');
    expect(Array.from(getSliderWrapper().classList)).to.contain('custom-value');
  });

  it('should display raw preset value in input field when changing slider value', async () => {
    const slider = getSlider();
    slider.value = '4';
    slider.dispatchEvent(new CustomEvent('input'));
    slider.dispatchEvent(new CustomEvent('change'));
    await elementUpdated(editor);

    expect(getInput().value).to.equal('60px');
  });

  it('should dispatch event with preset when changing slider value', async () => {
    const slider = getSlider();
    slider.value = '4';
    slider.dispatchEvent(new CustomEvent('input'));
    slider.dispatchEvent(new CustomEvent('change'));
    await elementUpdated(editor);

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('var(--test-size-xl)');
  });

  it('should display raw preset value in input field if theme value matches a preset value', async () => {
    expect(getInput().value).to.equal('40px');
  });

  it('should display raw preset value in input field if theme value is a preset value', async () => {
    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(hostMetadata.selector, 'height', 'var(--test-size-xl)');
    editor.theme = updatedTheme;
    await elementUpdated(getInput());

    expect(getInput().value).to.equal('60px');
  });

  it('should select slider preset when entering a value that matches a preset', async () => {
    const input = getInput();
    input.value = '60px';
    input.dispatchEvent(new CustomEvent('change'));
    await elementUpdated(editor);

    expect(getSlider().value).to.equal('4');
    expect(Array.from(getSliderWrapper().classList)).to.not.contain('custom-value');
  });

  it('should mark slider as having a custom value when entering a value that does not match a preset', async () => {
    const input = getInput();
    input.value = '25px';
    input.dispatchEvent(new CustomEvent('change'));
    await elementUpdated(editor);

    // Internal model value is -1, but browsers represent invalid value as 0 instead
    expect(getSlider().value).to.equal('0');
    expect(Array.from(getSliderWrapper().classList)).to.contain('custom-value');
  });

  it('should dispatch event with preset value when entering a value that matches a preset', async () => {
    const input = getInput();
    input.value = '60px';
    input.dispatchEvent(new CustomEvent('change'));
    await elementUpdated(editor);

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('var(--test-size-xl)');
  });

  it('should dispatch event with custom value when entering a value that does not match a preset', async () => {
    const input = getInput();
    input.value = '25px';
    input.dispatchEvent(new CustomEvent('change'));
    await elementUpdated(editor);

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('25px');
  });

  it('should display clear button when property is modified', async () => {
    expect(isClearButtonVisible()).to.be.false;

    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(hostMetadata.selector, 'height', '35px', true);
    editor.theme = updatedTheme;
    await elementUpdated(editor);
    await elementUpdated(getInput());

    expect(isClearButtonVisible()).to.be.true;
  });

  it('should dispatch event with empty value when clearing value', () => {
    getClearButton().click();

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('');
  });
});
