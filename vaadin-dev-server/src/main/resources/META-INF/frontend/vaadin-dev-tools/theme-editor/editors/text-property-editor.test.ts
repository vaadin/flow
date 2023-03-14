import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { ComponentTheme } from '../model';
import { CssPropertyMetadata } from '../metadata/model';
import { testElementMetadata } from '../tests/utils';
import { TextPropertyEditor } from './text-property-editor';
import './text-property-editor';
import sinon from 'sinon';

const colorMetadata: CssPropertyMetadata = {
  propertyName: 'color',
  displayName: 'Color'
};

describe('text property editor', () => {
  let theme: ComponentTheme;
  let editor: TextPropertyEditor;
  let valueChangeSpy: sinon.SinonSpy;

  function getInput() {
    return editor.shadowRoot!.querySelector('input') as HTMLInputElement;
  }

  function cloneTheme() {
    const result = new ComponentTheme(testElementMetadata);
    result.addPropertyValues(theme.properties);
    return result;
  }

  beforeEach(async () => {
    theme = new ComponentTheme(testElementMetadata);
    theme.updatePropertyValue(null, 'color', 'black');
    valueChangeSpy = sinon.spy();

    editor = await fixture(html` <vaadin-dev-tools-theme-text-property-editor
      .theme=${theme}
      .propertyMetadata=${colorMetadata}
      @theme-property-value-change=${valueChangeSpy}
    >
    </vaadin-dev-tools-theme-text-property-editor>`);
  });

  it('should update input from theme', async () => {
    const input = getInput();

    expect(input.value).to.equal('black');

    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(null, 'color', 'red', true);
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    expect(input.value).to.equal('red');
  });

  it('should dispatch event when changing input value', () => {
    const input = getInput();
    input.value = 'red';
    input.dispatchEvent(new CustomEvent('change'));

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('red');
  });
});
