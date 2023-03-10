import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { ComponentTheme } from '../model';
import { CssPropertyMetadata } from '../metadata/model';
import { testElementMetadata } from '../tests/utils';
import { TextPropertyEditor } from './text-property-editor';
import './text-property-editor';

const colorMetadata: CssPropertyMetadata = {
  propertyName: 'color',
  displayName: 'Color'
};

describe('text property editor', () => {
  let theme: ComponentTheme;
  let editor: TextPropertyEditor;

  function cloneTheme() {
    const result = new ComponentTheme(testElementMetadata);
    result.addPropertyValues(theme.properties);
    return result;
  }

  beforeEach(async () => {
    theme = new ComponentTheme(testElementMetadata);
    theme.updatePropertyValue(null, 'color', 'black');

    editor = await fixture(html` <vaadin-dev-tools-theme-text-property-editor
      .theme=${theme}
      .propertyMetadata=${colorMetadata}
    >
    </vaadin-dev-tools-theme-text-property-editor>`);
  });

  it('should not show modified indicator if property value is not modified', () => {
    const modifiedIndicator = editor.shadowRoot!.querySelector('.property-name .modified');

    expect(modifiedIndicator).to.not.exist;
  });

  it('should show modified indicator if property value is modified', async () => {
    const updatedTheme = cloneTheme();
    updatedTheme.updatePropertyValue(null, 'color', 'red', true);
    editor.theme = updatedTheme;
    await elementUpdated(editor);

    const modifiedIndicator = editor.shadowRoot!.querySelector('.property-name .modified');

    expect(modifiedIndicator).to.exist;
  });
});
