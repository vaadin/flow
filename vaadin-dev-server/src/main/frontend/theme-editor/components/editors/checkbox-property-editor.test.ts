import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { ComponentTheme } from '../../model';
import { ComponentElementMetadata, CssPropertyMetadata } from '../../metadata/model';
import { testElementMetadata } from '../../tests/utils';
import { TextPropertyEditor } from './text-property-editor';
import './checkbox-property-editor';
import sinon from 'sinon';

const fontWeightMetadata: CssPropertyMetadata = {
  propertyName: 'font-weight',
  displayName: 'Bold',
  checkedValue: 'bold'
};
const labelMetadata: ComponentElementMetadata = {
  selector: 'test-element::part(label)',
  displayName: 'Label',
  properties: [fontWeightMetadata]
};

describe('checkbox property editor', () => {
  let theme: ComponentTheme;
  let editor: TextPropertyEditor;
  let valueChangeSpy: sinon.SinonSpy;

  function getCheckbox() {
    return editor.shadowRoot!.querySelector('input') as HTMLInputElement;
  }

  function cloneTheme() {
    const result = new ComponentTheme(testElementMetadata);
    result.addPropertyValues(theme.properties);
    return result;
  }

  beforeEach(async () => {
    theme = new ComponentTheme(testElementMetadata);
    theme.updatePropertyValue(labelMetadata.selector, 'font-weight', 'bold');
    valueChangeSpy = sinon.spy();

    editor = await fixture(html` <vaadin-dev-tools-theme-checkbox-property-editor
      .theme=${theme}
      .elementMetadata=${labelMetadata}
      .propertyMetadata=${fontWeightMetadata}
      @theme-property-value-change=${valueChangeSpy}
    >
    </vaadin-dev-tools-theme-checkbox-property-editor>`);
  });

  it('should update checkbox from theme', async () => {
    const input = getCheckbox();

    expect(input.checked).to.be.true;

    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(labelMetadata.selector, 'font-weight', '', true);
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    expect(input.checked).to.be.false;
  });

  it('should dispatch event when changing checkbox value', () => {
    const checkbox = getCheckbox();
    checkbox.click();

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('');

    valueChangeSpy.resetHistory();
    checkbox.click();

    expect(valueChangeSpy.calledOnce).to.be.true;
    expect(valueChangeSpy.args[0][0].detail.value).to.equal('bold');
  });
});
