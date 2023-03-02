import { aTimeout, elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { ThemeEditorState } from './model';
import { ThemeEditor } from './editor';
import './editor';
import { PickerOptions, PickerProvider } from '../component-picker';
import { metadataRegistry } from './metadata/registry';
import sinon from 'sinon';
import { themePreview } from './preview';
import { testElementMetadata } from './tests/utils';

describe('theme-editor', () => {
  let editor: ThemeEditor;
  let testElement: HTMLElement;
  let connectionMock: {
    sendThemeEditorRules: sinon.SinonSpy;
  };
  let getMetadataStub: sinon.SinonStub;

  async function editorFixture() {
    connectionMock = {
      sendThemeEditorRules: sinon.spy(() => {})
    };
    const pickerMock = {
      open: (options: PickerOptions) => {
        options.pickCallback({ nodeId: 1, uiId: 1, element: testElement });
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

  function findPropertyEditor(partName: string | null, propertyName: string) {
    const partTestId = partName || 'host';
    return editor
      .shadowRoot!.querySelector('.property-list')
      ?.shadowRoot!.querySelector(
        `.section[data-testid="${partTestId}"] .property-editor[data-testid="${propertyName}"]`
      )!;
  }

  async function editProperty(partName: string, propertyName: string, value: string) {
    const propertyEditor = findPropertyEditor(partName, propertyName);

    expect(propertyEditor).to.exist;

    const input = propertyEditor.shadowRoot!.querySelector('input')!;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    await elementUpdated(editor);
  }

  function getPropertyValue(partName: string, propertyName: string) {
    const propertyEditor = findPropertyEditor(partName, propertyName);
    const input = propertyEditor.shadowRoot!.querySelector('input')! as HTMLInputElement;
    return input.value;
  }

  function findPickerButton() {
    return editor.shadowRoot!.querySelector('.picker button');
  }

  function getTestElementStyles() {
    return {
      host: getComputedStyle(testElement),
      label: getComputedStyle(testElement.shadowRoot!.querySelector('[part="label"]')!)
    };
  }

  before(async () => {
    getMetadataStub = sinon.stub(metadataRegistry, 'getMetadata').returns(Promise.resolve(testElementMetadata));
  });

  after(() => {
    getMetadataStub.restore();
  });

  beforeEach(async () => {
    themePreview.reset();
    testElement = await fixture(html` <test-element></test-element>`);
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
    it('should update theme preview after changing a property', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(getTestElementStyles().label.color).to.equal('rgb(255, 0, 0)');
    });

    it('should keep saved modifications in theme preview', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');
      expect(getTestElementStyles().label.color).to.equal('rgb(255, 0, 0)');

      await pickComponent();
      expect(getTestElementStyles().label.color).to.equal('rgb(255, 0, 0)');
    });

    it('should show property editors for picked component', async () => {
      await pickComponent();

      // Host properties
      testElementMetadata.properties.forEach((property) => {
        const propertyEditor = findPropertyEditor(null, property.propertyName);
        expect(propertyEditor).to.exist;
      });
      // Part properties
      testElementMetadata.parts.forEach((part) => {
        part.properties.forEach((property) => {
          const propertyEditor = findPropertyEditor(part.partName, property.propertyName);
          expect(propertyEditor).to.exist;
        });
      });
    });

    it('should initialize property editors with base theme values', async () => {
      await pickComponent();

      expect(getPropertyValue('label', 'color')).to.equal('rgb(0, 0, 0)');
    });

    it('should update property editors with edited theme values', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(getPropertyValue('label', 'color')).to.equal('red');
    });

    it('should keep previously saved theme modifications', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');
      await elementUpdated(editor);
      expect(getPropertyValue('label', 'color')).to.equal('red');

      await pickComponent();
      expect(getPropertyValue('label', 'color')).to.equal('red');
    });

    it('should send theme rules when changing properties', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(connectionMock.sendThemeEditorRules.calledOnce);
      expect(connectionMock.sendThemeEditorRules.args[0][0]).to.deep.equal([
        {
          selector: 'test-element::part(label)',
          property: 'color',
          value: 'red'
        }
      ]);
      connectionMock.sendThemeEditorRules.resetHistory();

      await editProperty('label', 'color', 'green');
      expect(connectionMock.sendThemeEditorRules.calledOnce);
      expect(connectionMock.sendThemeEditorRules.args[0][0]).to.deep.equal([
        {
          selector: 'test-element::part(label)',
          property: 'color',
          value: 'green'
        }
      ]);
    });

    it('should dispatch event before saving changes', async () => {
      const beforeSaveSpy = sinon.spy();
      editor.addEventListener('before-save', beforeSaveSpy);

      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(beforeSaveSpy.calledOnce).to.be.true;
    });
  });
});
