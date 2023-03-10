import { aTimeout, elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { ThemeEditorLicense, ThemeEditorSettings, ThemeEditorState } from './model';
import { ThemeEditor } from './editor';
import './editor';
import { PickerOptions, PickerProvider } from '../component-picker';
import { metadataRegistry } from './metadata/registry';
import sinon from 'sinon';
import { themePreview } from './preview';
import { testElementMetadata } from './tests/utils';
import { ThemeEditorApi } from './api';
import { ThemeEditorHistory } from './history';

describe('theme-editor', () => {
  let editor: ThemeEditor;
  let testElement: HTMLElement;
  let connectionMock: {
    onMessage: sinon.SinonSpy;
    send: sinon.SinonSpy;
  };
  let apiMock: {
    setCssRules: sinon.SinonStub;
    loadPreview: sinon.SinonStub;
    loadRules: sinon.SinonStub;
    undo: sinon.SinonStub;
    redo: sinon.SinonStub;
  };
  let historySpy: {
    push: sinon.SinonSpy;
  };
  let getMetadataStub: sinon.SinonStub;
  let beforeSaveSpy: sinon.SinonSpy;

  async function editorFixture() {
    connectionMock = {
      onMessage: sinon.spy(),
      send: sinon.spy()
    };
    const pickerMock = {
      open: (options: PickerOptions) => {
        options.pickCallback({ nodeId: 1, uiId: 1, element: testElement });
      }
    };
    const pickerProvider: PickerProvider = () => pickerMock as any;
    const settings: ThemeEditorSettings = {
      state: ThemeEditorState.enabled,
      license: ThemeEditorLicense.ok
    };
    const editor = (await fixture(html` <vaadin-dev-tools-theme-editor
      .settings=${settings}
      .pickerProvider=${pickerProvider}
      .connection=${connectionMock}
    ></vaadin-dev-tools-theme-editor>`)) as ThemeEditor;

    apiMock = {
      setCssRules: sinon.stub((editor as any).api as ThemeEditorApi, 'setCssRules'),
      loadPreview: sinon.stub((editor as any).api as ThemeEditorApi, 'loadPreview'),
      loadRules: sinon.stub((editor as any).api as ThemeEditorApi, 'loadRules'),
      undo: sinon.stub((editor as any).api as ThemeEditorApi, 'undo'),
      redo: sinon.stub((editor as any).api as ThemeEditorApi, 'redo')
    };
    apiMock.setCssRules.returns(Promise.resolve({}));
    apiMock.loadPreview.returns(Promise.resolve({ css: '' }));
    apiMock.loadRules.returns(Promise.resolve({ rules: [] }));
    apiMock.undo.returns(Promise.resolve({}));
    apiMock.redo.returns(Promise.resolve({}));

    historySpy = {
      push: sinon.spy((editor as any).history as ThemeEditorHistory, 'push')
    };

    beforeSaveSpy = sinon.spy();
    editor.addEventListener('before-save', beforeSaveSpy);

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
    return editor.shadowRoot!.querySelector('.picker button') as HTMLElement;
  }

  function findUndoButton() {
    return editor.shadowRoot!.querySelector('[data-testid="undo"]') as HTMLElement;
  }

  function findRedoButton() {
    return editor.shadowRoot!.querySelector('[data-testid="redo"]') as HTMLElement;
  }

  async function undo() {
    findUndoButton().click();
    await elementUpdated(editor);
    // Wait for async state updates to resolve
    await aTimeout(0);
  }

  async function redo() {
    findRedoButton().click();
    await elementUpdated(editor);
    // Wait for async state updates to resolve
    await aTimeout(0);
  }

  function getTestElementStyles() {
    return {
      host: getComputedStyle(testElement),
      label: getComputedStyle(testElement.shadowRoot!.querySelector('[part="label"]')!)
    };
  }

  function mockRulesResponse(partName: string, propertyName: string, value: string) {
    return Promise.resolve({
      rules: [
        {
          selector: `test-element::part(${partName})`,
          properties: {
            [propertyName]: value
          }
        }
      ]
    });
  }

  function mockPreviewResponse(css: string) {
    return Promise.resolve({
      css
    });
  }

  before(async () => {
    getMetadataStub = sinon.stub(metadataRegistry, 'getMetadata').returns(Promise.resolve(testElementMetadata));
  });

  after(() => {
    getMetadataStub.restore();
  });

  beforeEach(async () => {
    // Reset history
    ThemeEditorHistory.clear();
    // Reset theme preview
    themePreview.update('');
    // Render editable test element
    testElement = await fixture(html` <test-element></test-element>`);
    // Render editor
    const fixtureResult = await editorFixture();
    editor = fixtureResult.editor;
  });

  describe('theme editor states', () => {
    it('should show component picker in default state', async () => {
      expect(findPickerButton()).to.exist;
      expect(editor.shadowRoot!.innerHTML).to.not.contain('It looks like you have not set up a custom theme yet');
    });

    it('should show missing theme notice in theme missing state', async () => {
      editor.settings = {
        state: ThemeEditorState.missing_theme,
        license: ThemeEditorLicense.ok
      };
      await elementUpdated(editor);

      expect(findPickerButton()).to.not.exist;
      expect(editor.shadowRoot!.innerHTML).to.contain('It looks like you have not set up a custom theme yet');
    });
  });

  describe('editing', () => {
    it('should update theme preview after changing a property', async () => {
      apiMock.loadPreview.returns(
        Promise.resolve({
          css: 'test-element::part(label) { color: red }'
        })
      );
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(apiMock.loadPreview.calledOnce).to.be.true;
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

    it('should initialize property editors with base theme values when there are no custom rules', async () => {
      await pickComponent();

      expect(getPropertyValue('label', 'color')).to.equal('rgb(0, 0, 0)');
    });

    it('should initialize property editors with values from custom rules', async () => {
      apiMock.loadRules.returns(
        Promise.resolve({
          rules: [
            {
              selector: 'test-element::part(label)',
              properties: {
                color: 'red'
              }
            }
          ]
        })
      );
      await pickComponent();

      expect(getPropertyValue('label', 'color')).to.equal('red');
    });

    it('should update property editors with edited theme values', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(getPropertyValue('label', 'color')).to.equal('red');
    });

    it('should mark property as modified', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      const modifiedIndicator = findPropertyEditor('label', 'color').shadowRoot!.querySelector(
        '.property-name .modified'
      );
      expect(modifiedIndicator).to.exist;
    });

    it('should send theme rules when changing properties', async () => {
      await pickComponent();

      await editProperty('label', 'color', 'red');
      expect(apiMock.setCssRules.calledOnce).to.be.true;
      expect(apiMock.setCssRules.args[0][0]).to.deep.equal([
        {
          selector: 'test-element::part(label)',
          properties: { color: 'red' }
        }
      ]);
      apiMock.setCssRules.resetHistory();

      await editProperty('label', 'color', 'green');
      expect(apiMock.setCssRules.calledOnce).to.be.true;
      expect(apiMock.setCssRules.args[0][0]).to.deep.equal([
        {
          selector: 'test-element::part(label)',
          properties: { color: 'green' }
        }
      ]);
    });

    it('should dispatch event before saving changes', async () => {
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(beforeSaveSpy.calledOnce).to.be.true;
    });
  });

  describe('undo and redo', () => {
    it('should only enable undo and redo buttons when history allows it', async () => {
      const undoButton = findUndoButton();
      const redoButton = findRedoButton();

      // disabled initially
      expect(undoButton.getAttribute('disabled')).to.not.be.null;
      expect(redoButton.getAttribute('disabled')).to.not.be.null;

      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // undo allowed
      expect(undoButton.getAttribute('disabled')).to.be.null;
      expect(redoButton.getAttribute('disabled')).to.not.be.null;

      // undo
      await undo();

      // redo allowed
      expect(undoButton.getAttribute('disabled')).to.not.be.null;
      expect(redoButton.getAttribute('disabled')).to.be.null;
    });

    it('should add history entry when editing property', async () => {
      apiMock.setCssRules.returns(Promise.resolve({ requestId: 'request1' }));

      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      expect(historySpy.push.calledOnce).to.be.true;
      expect(historySpy.push.args).to.deep.equal([['request1']]);
    });

    it('should call undo when clicking undo button', async () => {
      apiMock.setCssRules.returns(Promise.resolve({ requestId: 'request1' }));

      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // undo
      await undo();

      expect(apiMock.undo.calledOnce).to.be.true;
      expect(apiMock.undo.args).to.deep.equal([['request1']]);
    });

    it('should refresh theme on undo', async () => {
      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // mock theme responses
      const rulesResponse = mockRulesResponse('label', 'color', 'green');
      apiMock.loadRules.returns(rulesResponse);
      const previewResponse = mockPreviewResponse('test-element::part(label) { color: green }');
      apiMock.loadPreview.returns(previewResponse);

      // undo
      await undo();

      expect(getPropertyValue('label', 'color')).to.equal('green');
      expect(getTestElementStyles().label.color).to.equal('rgb(0, 128, 0)');
    });

    it('should prevent live reload on undo', async () => {
      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // undo
      beforeSaveSpy.resetHistory();
      await undo();

      expect(beforeSaveSpy.calledOnce).to.be.true;
    });

    it('should call redo when clicking redo button', async () => {
      apiMock.setCssRules.returns(Promise.resolve({ requestId: 'request1' }));

      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // undo
      await undo();

      // redo
      await redo();

      expect(apiMock.redo.calledOnce).to.be.true;
      expect(apiMock.redo.args).to.deep.equal([['request1']]);
    });

    it('should refresh theme on redo', async () => {
      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // undo
      await undo();

      // mock theme responses
      const rulesResponse = mockRulesResponse('label', 'color', 'green');
      apiMock.loadRules.returns(rulesResponse);
      const previewResponse = mockPreviewResponse('test-element::part(label) { color: green }');
      apiMock.loadPreview.returns(previewResponse);

      // redo
      await redo();

      expect(getPropertyValue('label', 'color')).to.equal('green');
      expect(getTestElementStyles().label.color).to.equal('rgb(0, 128, 0)');
    });

    it('should prevent live reload on redo', async () => {
      // edit something
      await pickComponent();
      await editProperty('label', 'color', 'red');

      // undo
      await undo();

      // redo
      beforeSaveSpy.resetHistory();
      await redo();

      expect(beforeSaveSpy.calledOnce).to.be.true;
    });
  });
});
