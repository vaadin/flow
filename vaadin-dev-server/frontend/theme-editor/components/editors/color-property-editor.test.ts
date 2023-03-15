import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import sinon from 'sinon';
import '@vaadin/overlay';
import { CssPropertyMetadata } from '../../metadata/model';
import { ComponentTheme } from '../../model';
import { testElementMetadata } from '../../tests/utils';
import { ColorPropertyEditor } from './color-property-editor';
import './color-property-editor';
import { ColorPicker, ColorPickerChangeEvent } from './color-picker';

const colorMetadata: CssPropertyMetadata = {
  propertyName: 'color',
  displayName: 'Color',
  presets: ['--test-primary-color', '--test-success-color', '--test-error-color']
};

const backgroundColorMetadata: CssPropertyMetadata = {
  propertyName: 'background-color',
  displayName: 'Background color',
  presets: ['yellow', 'orange', 'brown']
};

describe('color property editor', () => {
  let theme: ComponentTheme;
  let editor: ColorPropertyEditor;
  let valueChangeSpy: sinon.SinonSpy;

  beforeEach(async () => {
    // Define custom CSS properties
    await fixture(html` <style>
      html {
        --test-primary-color: #00f;
        --test-success-color: #0f0;
        --test-error-color: #f00;
      }
    </style>`);

    theme = new ComponentTheme(testElementMetadata);
    theme.updatePropertyValue(null, 'color', 'black');
    theme.updatePropertyValue(null, 'background-color', 'white');
    valueChangeSpy = sinon.spy();

    editor = await fixture(html` <vaadin-dev-tools-theme-color-property-editor
      .theme=${theme}
      .propertyMetadata=${colorMetadata}
      @theme-property-value-change=${valueChangeSpy}
    >
    </vaadin-dev-tools-theme-color-property-editor>`);
  });

  function getInput() {
    return editor.shadowRoot!.querySelector('input') as HTMLInputElement;
  }

  function getColorPicker() {
    return editor.shadowRoot!.querySelector('vaadin-dev-tools-color-picker') as ColorPicker;
  }

  function cloneTheme() {
    const result = new ComponentTheme(testElementMetadata);
    result.addPropertyValues(theme.properties);
    return result;
  }

  it('should display property value from theme', () => {
    expect(getInput().value).to.equal('black');
    expect(getColorPicker().value).to.equal('black');
  });

  it('should update value when theme changes', async () => {
    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(null, 'color', 'red');
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    expect(getInput().value).to.equal('red');
    expect(getColorPicker().value).to.equal('red');
  });

  it('should display raw preset value if theme value is a preset value', async () => {
    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(null, 'color', 'var(--test-primary-color)');
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    expect(getInput().value).to.equal('rgb(0, 0, 255)');
    expect(getColorPicker().value).to.equal('rgb(0, 0, 255)');
  });

  it('should update value when metadata changes', async () => {
    editor.propertyMetadata = backgroundColorMetadata;
    await elementUpdated(editor);

    expect(getInput().value).to.equal('white');
    expect(getColorPicker().value).to.equal('white');
  });

  it('should pass presets to color picker', () => {
    expect(getColorPicker().presets).to.deep.equal([
      'var(--test-primary-color)',
      'var(--test-success-color)',
      'var(--test-error-color)'
    ]);
  });

  it('should update presets when metadata changes', async () => {
    editor.propertyMetadata = backgroundColorMetadata;
    await elementUpdated(editor);

    expect(getColorPicker().presets).to.deep.equal(['yellow', 'orange', 'brown']);
  });

  it('should update value when color picker changes', async () => {
    getColorPicker().dispatchEvent(new ColorPickerChangeEvent('red'));
    await elementUpdated(editor);

    expect(getInput().value).to.equal('red');
    expect(getColorPicker().value).to.equal('red');
  });

  it('should revert value when color picker changes and cancels', async () => {
    getColorPicker().dispatchEvent(new ColorPickerChangeEvent('red'));
    await elementUpdated(editor);

    expect(getInput().value).to.equal('red');
    expect(getColorPicker().value).to.equal('red');

    getColorPicker().dispatchEvent(new CustomEvent('color-picker-cancel'));
    await elementUpdated(editor);

    expect(getInput().value).to.equal('black');
    expect(getColorPicker().value).to.equal('black');
  });

  it('should not dispatch change event when color picker changes', () => {
    getColorPicker().dispatchEvent(new ColorPickerChangeEvent('yellow'));

    expect(valueChangeSpy.called).to.be.false;
  });

  it('should not dispatch change event when color picker changes and cancels', () => {
    getColorPicker().dispatchEvent(new ColorPickerChangeEvent('yellow'));
    getColorPicker().dispatchEvent(new CustomEvent('color-picker-cancel'));

    expect(valueChangeSpy.called).to.be.false;
  });

  it('should dispatch change event when color picker changes and commits', () => {
    getColorPicker().dispatchEvent(new ColorPickerChangeEvent('yellow'));
    getColorPicker().dispatchEvent(new CustomEvent('color-picker-commit'));

    expect(valueChangeSpy.called).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.be.equal('yellow');
  });

  it('should dispatch change event with preset value when color picker commits a preset', () => {
    getColorPicker().dispatchEvent(new ColorPickerChangeEvent('var(--test-error-color)'));
    getColorPicker().dispatchEvent(new CustomEvent('color-picker-commit'));

    expect(valueChangeSpy.called).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.be.equal('var(--test-error-color)');
  });

  it('should dispatch change event when entering a value', () => {
    const input = getInput();
    input.value = 'yellow';
    input.dispatchEvent(new CustomEvent('change'));

    expect(valueChangeSpy.called).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.be.equal('yellow');
  });

  it('should dispatch change event with preset value when entering a value that matches a preset', () => {
    const input = getInput();
    input.value = 'rgb(255, 0, 0)';
    input.dispatchEvent(new CustomEvent('change'));

    expect(valueChangeSpy.called).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.be.equal('var(--test-error-color)');
  });
});
