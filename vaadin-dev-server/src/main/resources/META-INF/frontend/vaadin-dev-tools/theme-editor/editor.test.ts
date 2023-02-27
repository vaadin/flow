import { aTimeout, elementUpdated, expect, fixture, html } from '@open-wc/testing';
import '@vaadin/button';
import { ThemeEditorRule, ThemeEditorState } from './model';
import { ThemeEditor } from './editor';
import './editor';
import { PickerOptions, PickerProvider } from '../component-picker';
import { metadataRegistry } from './metadata/registry';
import buttonMetadata from './metadata/components/vaadin-button';
import sinon from 'sinon';
import {themePreview} from "./preview";

describe('theme-editor', () => {
  let editor: ThemeEditor;
  let button: HTMLElement;
  let defaultButtonLabelStyles: CSSStyleDeclaration;
  let connectionMock: {
    sendThemeEditorRules: sinon.SinonSpy;
  };

  async function editorFixture() {
    connectionMock = {
      sendThemeEditorRules: sinon.spy(() => {})
    };
    const pickerMock = {
      open: (options: PickerOptions) => {
        options.pickCallback({ nodeId: 1, uiId: 1, element: button });
      }
    };
    const pickerProvider: PickerProvider = () => pickerMock as any;
    const editor = (await fixture(html` <vaadin-dev-tools-theme-editor
      .pickerProvider=${pickerProvider}
      .connection=${connectionMock}
    ></vaadin-dev-tools-theme-editor>`)) as ThemeEditor;

    return {
      editor,
      pickerProvider
    };
  }

  async function pickComponent() {
    const pickerButton = editor.shadowRoot!.querySelector('.picker button') as HTMLButtonElement;
    pickerButton.click();
    // Compensate for dynamic import of component picker
    await aTimeout(50);
  }

  function findPropertyEditor(partName: string, propertyName: string) {
    return editor
      .shadowRoot!.querySelector('.property-list')
      ?.shadowRoot!.querySelector(`.part[data-testid="${partName}"] .property-editor[data-testid="${propertyName}"]`)!;
  }

  async function editProperty(partName: string, propertyName: string, value: string) {
    const propertyEditor = findPropertyEditor(partName, propertyName);

    expect(propertyEditor).to.exist;

    const input = propertyEditor.shadowRoot!.querySelector('input')!;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    await elementUpdated(editor);
  }

  function findPickerButton() {
    return editor.shadowRoot!.querySelector('.picker button');
  }

  function findDiscardButton() {
    return editor.shadowRoot!.querySelector('.modifications-actions button.discard') as HTMLElement;
  }

  function findApplyButton() {
    return editor.shadowRoot!.querySelector('.modifications-actions button.apply') as HTMLElement;
  }

  before(async () => {
    // Pre-cache button metadata
    const button = document.createElement('vaadin-button');
    await metadataRegistry.getMetadata({ nodeId: 1, uiId: 1, element: button });
  });

  beforeEach(async () => {
    themePreview.reset();
    button = await fixture(html` <vaadin-button></vaadin-button>`);
    defaultButtonLabelStyles = getComputedStyle(button.shadowRoot!.querySelector('[part="label"]')!);
    const fixtureResult = await editorFixture();
    editor = fixtureResult.editor;
  });

  describe('theme editor states', () => {
    it('should show component picker in default state', async () => {
      expect(findPickerButton()).to.exist;
      expect(editor.shadowRoot!.innerHTML).to.not.contain('It looks like you have not set up a custom theme yet');
    });

    it('should show missing theme notice in theme missing state', async () => {
      editor.themeEditorState = ThemeEditorState.missing_theme;
      await elementUpdated(editor);

      expect(findPickerButton()).to.not.exist;
      expect(editor.shadowRoot!.innerHTML).to.contain('It looks like you have not set up a custom theme yet');
    });
  });

  describe('editing', () => {
    it('should not be modified initially', async () => {
      expect(findDiscardButton()).to.not.exist;
      expect(findApplyButton()).to.not.exist;
    });

    it('should be modified after changing a property', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(findDiscardButton()).to.exist;
      expect(findApplyButton()).to.exist;
    });

    it('should not be modified after discarding changes', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      findDiscardButton().click();
      await elementUpdated(editor);

      expect(findDiscardButton()).to.not.exist;
      expect(findApplyButton()).to.not.exist;
    });

    it('should not be modified after picking another component', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      await pickComponent();

      expect(findDiscardButton()).to.not.exist;
      expect(findApplyButton()).to.not.exist;
    });

    it('should update theme preview after changing a property', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      const labelStyle = getComputedStyle(button.shadowRoot!.querySelector('[part="label"]')!);
      expect(labelStyle.color).to.equal('rgb(255, 0, 0)');
    });

    it('should reset theme preview after discarding changes', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      findDiscardButton().click();
      await elementUpdated(editor);

      const labelStyle = getComputedStyle(button.shadowRoot!.querySelector('[part="label"]')!);
      expect(labelStyle.color).to.equal(defaultButtonLabelStyles.color);
    });

    it('should reset theme preview after picking another component', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      await pickComponent();

      const labelStyle = getComputedStyle(button.shadowRoot!.querySelector('[part="label"]')!);
      expect(labelStyle.color).to.equal(defaultButtonLabelStyles.color);
    });

    it('should show property editors for picked component', async () => {
      await pickComponent();

      buttonMetadata.parts.forEach((part) => {
        part.properties.forEach((property) => {
          const propertyEditor = findPropertyEditor(part.partName, property.propertyName);
          expect(propertyEditor).to.exist;
        });
      });
    });

    it('should initialize property editors with base theme values', async () => {
      await pickComponent();

      const propertyEditor = findPropertyEditor('label', 'color');
      const input = propertyEditor.shadowRoot!.querySelector('input')! as HTMLInputElement;

      expect(input.value).to.equal(defaultButtonLabelStyles.color);
    });

    it('should update property editors with edited theme values', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      const propertyEditor = findPropertyEditor('label', 'color');
      const input = propertyEditor.shadowRoot!.querySelector('input')! as HTMLInputElement;

      expect(input.value).to.equal('red');
    });

    it('should reset property editors after discarding changes', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      findDiscardButton().click();
      await elementUpdated(editor);

      const propertyEditor = findPropertyEditor('label', 'color');
      const input = propertyEditor.shadowRoot!.querySelector('input')! as HTMLInputElement;

      expect(input.value).to.equal(defaultButtonLabelStyles.color);
    });

    it('should reset property editors after picking another component', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      await pickComponent();

      const propertyEditor = findPropertyEditor('label', 'color');
      const input = propertyEditor.shadowRoot!.querySelector('input')! as HTMLInputElement;

      expect(input.value).to.equal(defaultButtonLabelStyles.color);
    });

    it('should send theme rules when applying changes', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      findApplyButton().click();

      const expectedRules: ThemeEditorRule[] = [
        {
          selector: 'vaadin-button::part(label)',
          property: 'color',
          value: 'red'
        }
      ];

      expect(connectionMock.sendThemeEditorRules.called);
      expect(connectionMock.sendThemeEditorRules.args[0][0]).to.deep.equal(expectedRules);
    });
  });
});
