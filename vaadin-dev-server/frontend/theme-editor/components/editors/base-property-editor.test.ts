import { TemplateResult } from 'lit';
import { customElement } from 'lit/decorators.js';
import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { ComponentTheme } from '../../model';
import { ComponentElementMetadata, CssPropertyMetadata } from '../../metadata/model';
import { testElementMetadata } from '../../tests/utils';
import { BasePropertyEditor } from './base-property-editor';

@customElement('test-property-editor')
class TestPropertyEditor extends BasePropertyEditor {
  protected renderEditor(): TemplateResult {
    return html`<input .value="${this.value}" />`;
  }
}

const colorMetadata: CssPropertyMetadata = {
  propertyName: 'color',
  displayName: 'Color'
};

const labelMetadata: ComponentElementMetadata = {
  selector: 'test-element::part(label)',
  displayName: 'Label',
  properties: [colorMetadata]
};

describe('base property editor', () => {
  let theme: ComponentTheme;
  let editor: TestPropertyEditor;

  function cloneTheme() {
    const result = new ComponentTheme(testElementMetadata);
    result.addPropertyValues(theme.properties);
    return result;
  }

  function getInput() {
    return editor.shadowRoot!.querySelector('input') as HTMLInputElement;
  }

  beforeEach(async () => {
    theme = new ComponentTheme(testElementMetadata);
    theme.updatePropertyValue(labelMetadata.selector, 'color', 'black');

    editor = await fixture(html` <test-property-editor
      .theme=${theme}
      .elementMetadata=${labelMetadata}
      .propertyMetadata=${colorMetadata}
    >
    </test-property-editor>`);
  });

  it('should update value property from theme', async () => {
    const input = getInput();

    expect(input.value).to.equal('black');

    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(labelMetadata.selector, 'color', 'red', true);
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    expect(input.value).to.equal('red');
  });

  it('should not show modified indicator if property value is not modified', () => {
    const modifiedIndicator = editor.shadowRoot!.querySelector('.label .modified');

    expect(modifiedIndicator).to.not.exist;
  });

  it('should show modified indicator if property value is modified', async () => {
    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(labelMetadata.selector, 'color', 'red', true);
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    const modifiedIndicator = editor.shadowRoot!.querySelector('.label .modified');

    expect(modifiedIndicator).to.exist;
  });
});
