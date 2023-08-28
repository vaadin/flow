import { aTimeout, elementUpdated, expect, fixture, html } from '@open-wc/testing';
import '@vaadin/select';
import { ThemeEditorState, ThemeScope } from './model';
import { ThemeEditor } from './editor';
import './editor';
import { PickerOptions, PickerProvider } from '../component-picker';
import { metadataRegistry } from './metadata/registry';
import sinon from 'sinon';
import { themePreview } from './preview';
import { testElementMetadata } from './tests/utils';
import { ThemeEditorApi } from './api';
import { ThemeEditorHistory } from './history';
import { ComponentReference } from '../component-util';
import { ScopeChangeEvent, ScopeSelector } from './components/scope-selector';

describe('theme-editor', () => {
  let editor: ThemeEditor;
  let testElement: HTMLElement;
  let testComponentRef: ComponentReference;
  let connectionMock: {
    onMessage: sinon.SinonSpy;
    send: sinon.SinonSpy;
  };
  let apiMock: {
    loadComponentMetadata: sinon.SinonStub;
    setLocalClassName: sinon.SinonStub;
    setCssRules: sinon.SinonStub;
    loadRules: sinon.SinonStub;
    undo: sinon.SinonStub;
    redo: sinon.SinonStub;
    openCss: sinon.SinonStub;
    markAsUsed: sinon.SinonStub;
  };
  let historySpy: {
    push: sinon.SinonSpy;
  };
  let getMetadataStub: sinon.SinonStub;
  let mockTheme: CSSStyleSheet;

  async function editorFixture() {
    connectionMock = {
      onMessage: sinon.spy(),
      send: sinon.spy()
    };
    testComponentRef = { nodeId: 123, uiId: 456, element: testElement };
    const pickerMock = {
      open: (options: PickerOptions) => {
        options.pickCallback(testComponentRef);
      }
    };
    const pickerProvider: PickerProvider = () => pickerMock as any;
    const editor = (await fixture(html` <vaadin-dev-tools-theme-editor
      .expanded=${true}
      .pickerProvider=${pickerProvider}
      .connection=${connectionMock}
    ></vaadin-dev-tools-theme-editor>`)) as ThemeEditor;

    apiMock = {
      loadComponentMetadata: sinon.stub((editor as any).api as ThemeEditorApi, 'loadComponentMetadata'),
      setLocalClassName: sinon.stub((editor as any).api as ThemeEditorApi, 'setLocalClassName'),
      setCssRules: sinon.stub((editor as any).api as ThemeEditorApi, 'setCssRules'),
      loadRules: sinon.stub((editor as any).api as ThemeEditorApi, 'loadRules'),
      undo: sinon.stub((editor as any).api as ThemeEditorApi, 'undo'),
      redo: sinon.stub((editor as any).api as ThemeEditorApi, 'redo'),
      openCss: sinon.stub((editor as any).api as ThemeEditorApi, 'openCss'),
      markAsUsed: sinon.stub((editor as any).api as ThemeEditorApi, 'markAsUsed')
    };
    apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: true, className: 'test-class' }));
    apiMock.setLocalClassName.returns(Promise.resolve({}));
    apiMock.setCssRules.returns(Promise.resolve({}));
    apiMock.loadRules.returns(Promise.resolve({ rules: [], accessible: true }));
    apiMock.undo.returns(Promise.resolve({}));
    apiMock.redo.returns(Promise.resolve({}));
    apiMock.openCss.returns(Promise.resolve({}));
    apiMock.markAsUsed.returns(Promise.resolve({}));

    historySpy = {
      push: sinon.spy((editor as any).history as ThemeEditorHistory, 'push')
    };

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

  function findPropertyEditor(elementName: string, propertyName: string) {
    return editor
      .shadowRoot!.querySelector('.property-list')
      ?.shadowRoot!.querySelector(
        `.section[data-testid="${elementName}"] .property-editor[data-testid="${propertyName}"]`
      )!;
  }

  async function editProperty(elementName: string, propertyName: string, value: string) {
    const propertyEditor = findPropertyEditor(elementName, propertyName);

    expect(propertyEditor).to.exist;

    const input = propertyEditor
      .shadowRoot!.querySelector('vaadin-dev-tools-theme-text-input')!
      .shadowRoot!.querySelector('input') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    await elementUpdated(editor);
  }

  async function openCss(elementName: string) {
    const openCssButton = editor
      .shadowRoot!.querySelector('.property-list')
      ?.shadowRoot!.querySelector(`.section[data-testid="${elementName}"] button.open-css`) as HTMLElement;
    openCssButton.click();
    // Wait for async state updates to resolve
    await aTimeout(0);
  }

  function getPropertyValue(elementName: string, propertyName: string) {
    const propertyEditor = findPropertyEditor(elementName, propertyName);
    const input = propertyEditor
      .shadowRoot!.querySelector('vaadin-dev-tools-theme-text-input')!
      .shadowRoot!.querySelector('input') as HTMLInputElement;
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

  async function changeThemeScope(scope: ThemeScope) {
    const scopeSelector = editor.shadowRoot!.querySelector('vaadin-dev-tools-theme-scope-selector') as ScopeSelector;
    scopeSelector.value = scope;
    scopeSelector.dispatchEvent(new ScopeChangeEvent(scope));
    await elementUpdated(editor);
    // Wait for async state updates to resolve
    await aTimeout(0);
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

  async function editLocalClassName(className: string) {
    const classNameEditor = editor.shadowRoot!.querySelector(
      '.header vaadin-dev-tools-theme-class-name-editor'
    ) as HTMLElement;
    const classNameInput = classNameEditor
      .shadowRoot!.querySelector('vaadin-dev-tools-theme-text-input')!
      .shadowRoot!.querySelector('input') as HTMLInputElement;

    classNameInput.value = className;
    classNameInput.dispatchEvent(new Event('change'));
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

  function mockRulesResponse(selector: string, propertyName: string, value: string) {
    return Promise.resolve({
      rules: [
        {
          selector,
          properties: {
            [propertyName]: value
          }
        }
      ]
    });
  }

  function mockThemeStyles(css: string) {
    mockTheme.replaceSync(css);
  }

  async function mockHmrUpdate() {
    document.dispatchEvent(new CustomEvent('vaadin-theme-updated'));
    // Wait for async state updates to resolve
    await aTimeout(0);
  }

  before(async () => {
    mockTheme = new CSSStyleSheet();
    document.adoptedStyleSheets = [...document.adoptedStyleSheets, mockTheme];
  });

  beforeEach(async () => {
    // Reset history
    ThemeEditorHistory.clear();
    // Reset mock theme styles
    mockTheme.replaceSync('');
    // Reset theme preview
    themePreview.clear();
    // Mock metadata
    getMetadataStub = sinon.stub(metadataRegistry, 'getMetadata').returns(Promise.resolve(testElementMetadata));
    // Render editable test element
    testElement = await fixture(html` <test-element></test-element>`);
    // Render editor
    const fixtureResult = await editorFixture();
    editor = fixtureResult.editor;
  });

  afterEach(() => {
    getMetadataStub.restore();
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
      expect(editor.shadowRoot!.innerHTML).to.contain('It looks like you have not set up an application theme yet');
    });
  });

  describe('editing', () => {
    it('should optimistically update theme preview after changing a property', async () => {
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      expect(getTestElementStyles().label.color).to.equal('rgb(255, 0, 0)');
    });

    it('should show property editors for picked component', async () => {
      await pickComponent();

      testElementMetadata.elements.forEach((element) => {
        element.properties.forEach((property) => {
          const propertyEditor = findPropertyEditor(element.displayName, property.propertyName);
          expect(propertyEditor).to.exist;
        });
      });
    });

    it('should initialize property editors with base theme values when there are no custom rules', async () => {
      await pickComponent();

      expect(getPropertyValue('Label', 'color')).to.equal('rgb(0, 0, 0)');
    });

    it('should initialize property editors with values from custom rules', async () => {
      apiMock.loadRules.returns(
        Promise.resolve({
          rules: [
            {
              selector: 'test-element.test-class::part(label)',
              properties: {
                color: 'red'
              }
            }
          ]
        })
      );
      await pickComponent();

      expect(getPropertyValue('Label', 'color')).to.equal('red');
    });

    it('should update property editors with edited theme values', async () => {
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      expect(getPropertyValue('Label', 'color')).to.equal('red');
    });

    it('should mark property as modified', async () => {
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      const modifiedIndicator = findPropertyEditor('Label', 'color').shadowRoot!.querySelector('.label .modified');
      expect(modifiedIndicator).to.exist;
    });

    it('should send theme rules when changing properties', async () => {
      await pickComponent();

      await editProperty('Label', 'color', 'red');
      expect(apiMock.setCssRules.calledOnce).to.be.true;
      expect(apiMock.setCssRules.args[0][0]).to.deep.equal([
        {
          selector: 'test-element.test-class::part(label)',
          properties: { color: 'red' }
        }
      ]);
      apiMock.setCssRules.resetHistory();

      await editProperty('Label', 'color', 'green');
      expect(apiMock.setCssRules.calledOnce).to.be.true;
      expect(apiMock.setCssRules.args[0][0]).to.deep.equal([
        {
          selector: 'test-element.test-class::part(label)',
          properties: { color: 'green' }
        }
      ]);
    });
  });

  describe('local scope', () => {
    it('should load rules for instance', async () => {
      await pickComponent();

      expect(apiMock.loadRules.calledOnce).to.be.true;
      expect(apiMock.loadRules.args).to.deep.equal([
        [
          [
            'test-element.test-class',
            'test-element.test-class::part(label)',
            'test-element.test-class input[slot="input"]',
            'test-element.test-class::part(helper-text)'
          ]
        ]
      ]);
    });

    it('should update rules for instance', async () => {
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      const expectedRules = [
        {
          selector: 'test-element.test-class::part(label)',
          properties: {
            color: 'red'
          }
        }
      ];

      expect(apiMock.setCssRules.calledOnce).to.be.true;
      expect(apiMock.setCssRules.args).to.deep.equal([[expectedRules]]);
    });

    it('should show notice if instance is inaccessible', async () => {
      apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: false }));
      await pickComponent();

      const propertyList = editor.shadowRoot!.querySelector('.property-list');
      expect(propertyList).to.not.exist;
      expect(editor.shadowRoot!.textContent).to.contain('The selected Test element cannot be styled locally');
    });

    it('should show local class name editor if instance is accessible', async () => {
      await pickComponent();

      const localClassNameEditor = editor.shadowRoot!.querySelector('.header vaadin-dev-tools-theme-class-name-editor');
      expect(localClassNameEditor).to.exist;
    });

    it('should not show local class name editor if instance is inaccessible', async () => {
      apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: false }));
      await pickComponent();

      const localClassNameEditor = editor.shadowRoot!.querySelector('.header vaadin-dev-tools-theme-class-name-editor');
      expect(localClassNameEditor).to.not.exist;
    });

    it('should not load rules if instance is inaccessible', async () => {
      apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: false }));
      await pickComponent();

      expect(apiMock.loadRules.calledOnce).to.be.false;
    });

    it('should not load rules if instance does not have local class name', async () => {
      apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: true }));
      await pickComponent();

      expect(apiMock.loadRules.calledOnce).to.be.false;
    });

    it('should render property list if instance does not have local class name', async () => {
      apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: true }));
      await pickComponent();

      findPropertyEditor('Label', 'color');
      findPropertyEditor('Input', 'color');
    });
  });

  describe('global scope', () => {
    it('should load rules for component type', async () => {
      await pickComponent();
      apiMock.loadRules.resetHistory();
      await changeThemeScope(ThemeScope.global);

      expect(apiMock.loadRules.calledOnce).to.be.true;
      expect(apiMock.loadRules.args).to.deep.equal([
        [
          [
            'test-element',
            'test-element::part(label)',
            'test-element input[slot="input"]',
            'test-element::part(helper-text)'
          ]
        ]
      ]);
    });

    it('should update global rules', async () => {
      await pickComponent();
      apiMock.loadRules.resetHistory();
      await changeThemeScope(ThemeScope.global);
      await editProperty('Label', 'color', 'red');

      const expectedRules = [
        {
          selector: 'test-element::part(label)',
          properties: {
            color: 'red'
          }
        }
      ];

      expect(apiMock.setCssRules.calledOnce).to.be.true;
      expect(apiMock.setCssRules.args).to.deep.equal([[expectedRules]]);
    });

    it('should not show local class name editor', async () => {
      await pickComponent();
      await changeThemeScope(ThemeScope.global);

      const localClassNameEditor = editor.shadowRoot!.querySelector('.header vaadin-dev-tools-theme-class-name-editor');
      expect(localClassNameEditor).to.not.exist;
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
      await editProperty('Label', 'color', 'red');

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
      await editProperty('Label', 'color', 'red');

      expect(historySpy.push.calledOnce).to.be.true;
      expect(historySpy.push.args).to.deep.equal([['request1']]);
    });

    it('should add history entry for setting class name when editing property', async () => {
      // Simulate selected component not having a class name yet
      apiMock.loadComponentMetadata.returns(
        Promise.resolve({
          accessible: true,
          suggestedClassName: 'suggested-class'
        })
      );
      apiMock.setLocalClassName.returns(Promise.resolve({ requestId: 'request1' }));
      apiMock.setCssRules.returns(Promise.resolve({ requestId: 'request2' }));

      // edit something
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      expect(historySpy.push.calledTwice).to.be.true;
      expect(historySpy.push.args[0][0]).to.deep.equal('request1');
      expect(historySpy.push.args[1][0]).to.deep.equal('request2');
    });

    it('should call undo when clicking undo button', async () => {
      apiMock.setCssRules.returns(Promise.resolve({ requestId: 'request1' }));

      // edit something
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      // undo
      await undo();

      expect(apiMock.undo.calledOnce).to.be.true;
      expect(apiMock.undo.args).to.deep.equal([['request1']]);
    });

    it('should refresh theme on undo', async () => {
      // edit something
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      // mock theme responses
      const rulesResponse = mockRulesResponse('test-element.test-class::part(label)', 'color', 'green');
      apiMock.loadRules.returns(rulesResponse);

      // undo
      await undo();

      expect(getPropertyValue('Label', 'color')).to.equal('green');
    });

    it('should call redo when clicking redo button', async () => {
      apiMock.setCssRules.returns(Promise.resolve({ requestId: 'request1' }));

      // edit something
      await pickComponent();
      await editProperty('Label', 'color', 'red');

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
      await editProperty('Label', 'color', 'red');

      // undo
      await undo();

      // mock theme responses
      const rulesResponse = mockRulesResponse('test-element.test-class::part(label)', 'color', 'green');
      apiMock.loadRules.returns(rulesResponse);

      // redo
      await redo();

      expect(getPropertyValue('Label', 'color')).to.equal('green');
    });
  });

  describe('theme detection', () => {
    it('should detect base theme when changing scope', async () => {
      await pickComponent();
      await changeThemeScope(ThemeScope.global);
      await editProperty('Label', 'color', 'red');
      mockThemeStyles('test-element::part(label) { color: red }');
      await changeThemeScope(ThemeScope.local);

      expect(getPropertyValue('Label', 'color')).to.equal('rgb(255, 0, 0)');
    });
  });

  describe('highlighting', () => {
    it('should highlight selected component', async () => {
      await pickComponent();
      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.true;
    });

    it('should update highlight when selecting a different component', async () => {
      await pickComponent();
      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.true;

      const anotherElement = (await fixture(html` <test-element></test-element>`)) as HTMLElement;
      testComponentRef = { nodeId: 123, uiId: 456, element: anotherElement };
      await pickComponent();

      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.false;
      expect(anotherElement.classList.contains('vaadin-theme-editor-highlight')).to.be.true;
    });

    it('should remove highlight when removing editor from DOM', async () => {
      await pickComponent();
      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.true;

      editor.remove();
      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.false;
    });

    it('should remove highlight when editor is closed', async () => {
      await pickComponent();

      editor.expanded = false;
      await elementUpdated(editor);
      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.false;
    });

    it('should restore highlight when editor is opened again', async () => {
      await pickComponent();

      editor.expanded = false;
      await elementUpdated(editor);
      editor.expanded = true;
      await elementUpdated(editor);
      expect(testElement.classList.contains('vaadin-theme-editor-highlight')).to.be.true;
    });
  });

  describe('applying local class name', () => {
    beforeEach(() => {
      // Simulate selected component not having a class name yet
      apiMock.loadComponentMetadata.returns(
        Promise.resolve({
          accessible: true,
          suggestedClassName: 'suggested-class'
        })
      );
    });

    it('should apply suggested class name before setting rules', async () => {
      await pickComponent();
      await editProperty('Label', 'color', 'red');

      // should make API call to apply class name
      expect(apiMock.setLocalClassName.calledOnce).to.be.true;
      expect(apiMock.setLocalClassName.args[0]).to.deep.equal([testComponentRef, 'suggested-class']);
      // should add class name to selected component
      expect(testElement.classList.contains('suggested-class')).to.be.true;
    });

    it('should add existing className from component metadata response to selected component', async () => {
      apiMock.loadComponentMetadata.returns({
        accessible: true,
        className: 'existing-class'
      });
      await pickComponent();

      expect(testElement.classList.contains('existing-class')).to.be.true;
    });

    it('should not add existing className from component metadata response to previously selected component', async () => {
      await pickComponent();

      const anotherElement = (await fixture(html` <test-element></test-element>`)) as HTMLElement;
      testComponentRef = { nodeId: 123, uiId: 456, element: anotherElement };
      apiMock.loadComponentMetadata.returns({
        accessible: true,
        className: 'existing-class'
      });
      await pickComponent();

      expect(testElement.classList.contains('existing-class')).to.be.false;
      expect(anotherElement.classList.contains('existing-class')).to.be.true;
    });
  });

  describe('editing local class name', () => {
    it('should only update suggested class name if element has no local class name yet', async () => {
      // Simulate selected component not having a class name yet
      apiMock.loadComponentMetadata.returns(
        Promise.resolve({
          accessible: true,
          suggestedClassName: 'suggested-class'
        })
      );
      await pickComponent();

      // Changing class name should not send any requests yet, but modify
      // suggested class name
      await editLocalClassName('custom-class');
      expect(apiMock.setLocalClassName.called).to.be.false;

      // Changing property should apply custom class name
      await editProperty('Label', 'color', 'red');
      expect(apiMock.setLocalClassName.calledOnce).to.be.true;
      expect(apiMock.setLocalClassName.args).to.deep.equal([[testComponentRef, 'custom-class']]);
    });

    it('should immediately update class name if element already has a local class name', async () => {
      await pickComponent();
      await editLocalClassName('custom-class');

      expect(apiMock.setLocalClassName.calledOnce).to.be.true;
      expect(apiMock.setLocalClassName.args).to.deep.equal([[testComponentRef, 'custom-class']]);
    });

    it('should update local class name preview on selected component', async () => {
      await pickComponent();
      expect(testElement.classList.contains('test-class')).to.be.true;

      await editLocalClassName('custom-class');
      expect(testElement.classList.contains('test-class')).to.be.false;
      expect(testElement.classList.contains('custom-class')).to.be.true;
    });

    it('should revert local class name on undo', async () => {
      await pickComponent();
      await editLocalClassName('custom-class');
      expect(testElement.classList.contains('custom-class')).to.be.true;

      // pick a different component
      const anotherElement = (await fixture(html` <test-element></test-element>`)) as HTMLElement;
      testComponentRef = { nodeId: 123, uiId: 456, element: anotherElement };
      await pickComponent();

      // undo should revert class name on previously selected component
      await undo();
      expect(testElement.classList.contains('custom-class')).to.be.false;
      expect(testElement.classList.contains('test-class')).to.be.true;
    });

    it('should restore local class name on redo', async () => {
      await pickComponent();
      await editLocalClassName('custom-class');
      expect(testElement.classList.contains('custom-class')).to.be.true;

      // pick a different component
      const anotherElement = (await fixture(html` <test-element></test-element>`)) as HTMLElement;
      testComponentRef = { nodeId: 123, uiId: 456, element: anotherElement };
      await pickComponent();

      // undo should revert class name on previously selected component
      await undo();
      expect(testElement.classList.contains('custom-class')).to.be.false;
      expect(testElement.classList.contains('test-class')).to.be.true;

      // redo should restore class name on previously selected component
      await redo();
      expect(testElement.classList.contains('custom-class')).to.be.true;
      expect(testElement.classList.contains('test-class')).to.be.false;
    });

    it('should not submit invalid class name', async () => {
      await pickComponent();
      await editLocalClassName('custom class');

      expect(apiMock.setLocalClassName.called).to.be.false;
    });
  });

  describe('open CSS', () => {
    describe('local scope', () => {
      it('should open CSS', async () => {
        await pickComponent();
        await openCss('Label');

        expect(apiMock.openCss.calledOnce).to.be.true;
        expect(apiMock.openCss.args).to.deep.equal([['test-element.test-class::part(label)']]);
      });

      it('should apply local class name before attempting to open CSS', async () => {
        // Simulate selected component not having a class name yet
        apiMock.loadComponentMetadata.returns(
          Promise.resolve({
            accessible: true,
            suggestedClassName: 'suggested-class'
          })
        );
        await pickComponent();
        await openCss('Label');

        expect(apiMock.setLocalClassName.calledOnce).to.be.true;
        expect(apiMock.setLocalClassName.args[0]).to.deep.equal([testComponentRef, 'suggested-class']);
        expect(apiMock.openCss.calledOnce).to.be.true;
        expect(apiMock.openCss.args).to.deep.equal([['test-element.suggested-class::part(label)']]);
      });

      it('should fail if local class name can not be applied', async () => {
        // Simulate selected component not having a class name and no suggestion
        apiMock.loadComponentMetadata.returns(
          Promise.resolve({
            accessible: true
          })
        );
        await pickComponent();

        expect(async () => openCss('Label')).to.throw;
      });
    });

    describe('global scope', () => {
      it('should open CSS', async () => {
        await pickComponent();
        await changeThemeScope(ThemeScope.global);
        await openCss('Label');

        expect(apiMock.openCss.calledOnce).to.be.true;
        expect(apiMock.openCss.args).to.deep.equal([['test-element::part(label)']]);
      });

      it('should not apply local class name before attempting to open CSS', async () => {
        // Simulate selected component not having a class name yet
        apiMock.loadComponentMetadata.returns(
          Promise.resolve({
            accessible: true,
            suggestedClassName: 'suggested-class'
          })
        );
        await pickComponent();
        await changeThemeScope(ThemeScope.global);
        await openCss('Label');

        expect(apiMock.setLocalClassName.called).to.be.false;
        expect(apiMock.openCss.calledOnce).to.be.true;
        expect(apiMock.openCss.args).to.deep.equal([['test-element::part(label)']]);
      });
    });
  });

  describe('picking components', () => {
    it('should show notice if there is no metadata', async () => {
      getMetadataStub.returns(Promise.resolve(null));
      await pickComponent();

      const notice = editor.shadowRoot!.querySelector('.notice') as HTMLElement;
      expect(notice).to.exist;
      expect(notice.textContent).to.contain('Styling <test-element> components is not supported at the moment.');

      const propertyList = editor.shadowRoot!.querySelector('.property-list');
      expect(propertyList).to.not.exist;
    });
  });

  describe('HMR updates', () => {
    it('should refresh base theme when HMR update is received', async () => {
      await pickComponent();
      expect(getPropertyValue('Label', 'color')).to.equal('rgb(0, 0, 0)');

      mockThemeStyles('test-element::part(label) { color: red }');
      await mockHmrUpdate();
      expect(getPropertyValue('Label', 'color')).to.equal('rgb(255, 0, 0)');
    });

    it('should clear theme preview when HMR update is received', async () => {
      themePreview.add('test-element::part(label) { color: red }');
      expect(getTestElementStyles().label.color).to.equal('rgb(255, 0, 0)');

      await mockHmrUpdate();
      expect(getTestElementStyles().label.color).to.equal('rgb(0, 0, 0)');
    });
  });
});
