import { fixture, html, expect } from '@open-wc/testing';
import { ThemeEditorState } from './model';
import { ThemeEditor } from './editor';
import './editor';

describe('theme-editor', () => {
  describe('theme editor states', () => {
    it('should show component picker in default state', async () => {
      const editor: ThemeEditor = await fixture(html` <vaadin-dev-tools-theme-editor></vaadin-dev-tools-theme-editor>`);
      const pickerButton = editor.shadowRoot!.querySelector('.picker button');

      expect(pickerButton).to.exist;
      expect(editor.shadowRoot!.innerHTML).to.not.contain('It looks like you have not set up a custom theme yet');
    });

    it('should show missing theme notice in theme missing state', async () => {
      const editor: ThemeEditor = await fixture(html` <vaadin-dev-tools-theme-editor
        .themeEditorState=${ThemeEditorState.missing_theme}
      ></vaadin-dev-tools-theme-editor>`);
      const pickerButton = editor.shadowRoot!.querySelector('.picker button');

      expect(pickerButton).to.not.exist;
      expect(editor.shadowRoot!.innerHTML).to.contain('It looks like you have not set up a custom theme yet');
    });
  });
});
