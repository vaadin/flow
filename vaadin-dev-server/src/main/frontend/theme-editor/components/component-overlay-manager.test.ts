import { aTimeout, elementUpdated, expect, fixture, html } from '@open-wc/testing';
// @ts-ignore
import sinon from 'sinon';
import './component-overlay-manager';
import { ThemeEditor } from '../editor';
import '../editor';
import { PickerOptions, PickerProvider } from '../../component-picker';
import { ComponentReference } from '../../component-util';
import VaadinComboBox from '../metadata/components/vaadin-combo-box';
import VaadinSelect from '../metadata/components/vaadin-select';
import VaadinDatePicker from '../metadata/components/vaadin-date-picker';
import VaadinMenuBar from '../metadata/components/vaadin-menu-bar';
import { ThemeEditorApi } from '../api';
import VaadinTimePicker from '../metadata/components/vaadin-time-picker';
import VaadinMultiSelectComboBox from '../metadata/components/vaadin-multi-select-combo-box';
import { componentOverlayManager } from './component-overlay-manager';

const componentsWithOverlay = [
  {
    name: 'vaadin-select',
    import: import('@vaadin/select'),
    html: html`<vaadin-select
      .items="${[
        { label: 'First', value: 'first' },
        { label: 'Second', value: 'second' }
      ]}"
    >
    </vaadin-select>`,
    metaData: VaadinSelect,
    overlayTagName: 'vaadin-select-overlay'
  },
  {
    name: 'vaadin-time-picker',
    import: import('@vaadin/time-picker'),
    html: html`<vaadin-time-picker></vaadin-time-picker>`,
    metaData: VaadinTimePicker,
    overlayTagName: 'vaadin-time-picker-overlay'
  },
  {
    name: 'vaadin-menu-bar',
    import: import('@vaadin/menu-bar'),
    html: html`<vaadin-menu-bar
      .items="${[{ text: 'Menu', children: [{ text: 'Sub Menu1' }, { text: 'Sub Menu2' }] }]}"
    ></vaadin-menu-bar>`,
    metaData: VaadinMenuBar,
    overlayTagName: 'vaadin-menu-bar-overlay'
  },
  {
    name: 'vaadin-date-picker',
    import: import('@vaadin/date-picker'),
    html: html`<vaadin-date-picker></vaadin-date-picker>`,
    metaData: VaadinDatePicker,
    overlayTagName: 'vaadin-date-picker-overlay'
  },
  {
    name: 'vaadin-combo-box',
    import: import('@vaadin/combo-box'),
    html: html`<vaadin-combo-box
      item-id-path="key"
      item-label-path="name"
      .items="${[
        { key: 'first', name: 'First' },
        { key: 'second', name: 'Second' },
        { key: 'third', name: 'Third' },
        { key: 'fourth', name: 'Fourth' }
      ]}"
    >
    </vaadin-combo-box>`,
    metaData: VaadinComboBox,
    overlayTagName: 'vaadin-combo-box-overlay'
  },
  {
    name: 'vaadin-multi-select-combo-box',
    import: import('@vaadin/multi-select-combo-box'),
    html: html`<vaadin-multi-select-combo-box
      item-id-path="key"
      item-label-path="name"
      .items="${[
        { key: 'first', name: 'First' },
        { key: 'second', name: 'Second' },
        { key: 'third', name: 'Third' },
        { key: 'fourth', name: 'Fourth' }
      ]}"
    >
    </vaadin-multi-select-combo-box>`,
    metaData: VaadinMultiSelectComboBox,
    overlayTagName: 'vaadin-multi-select-combo-box-overlay'
  }
];

describe('component overlay manager', () => {
  let editor: ThemeEditor;
  let currentComponentReference: ComponentReference;
  let apiMock: any;
  beforeEach(async () => {
    const connectionMock = {
      onMessage: sinon.spy(),
      send: sinon.spy()
    };
    const pickerMock = {
      open: (options: PickerOptions) => {
        options.pickCallback(currentComponentReference);
      }
    };

    const pickerProvider: PickerProvider = () => pickerMock as any;
    editor = (await fixture(html` <vaadin-dev-tools-theme-editor
      .expanded=${true}
      .pickerProvider="${pickerProvider}"
      .connection=${connectionMock}
    ></vaadin-dev-tools-theme-editor>`)) as ThemeEditor;

    apiMock = {
      loadComponentMetadata: sinon.stub((editor as any).api as ThemeEditorApi, 'loadComponentMetadata'),
      setLocalClassName: sinon.stub((editor as any).api as ThemeEditorApi, 'setLocalClassName'),
      setCssRules: sinon.stub((editor as any).api as ThemeEditorApi, 'setCssRules')
    };
    apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: true, className: 'test-class' }));
    apiMock.setLocalClassName.returns(Promise.resolve({}));
    apiMock.setCssRules.returns(Promise.resolve({}));
  });

  async function pickComponent(element: HTMLElement) {
    const pickerButton = editor.shadowRoot!.querySelector('.picker button') as HTMLButtonElement;
    pickerButton.click();
    await elementUpdated(editor);
    await elementUpdated(element);
    //waiting editor is updating
    await aTimeout(100);
  }

  componentsWithOverlay.forEach((componentDefinition) => {
    it(`Overlay must be visible after picking for the ${componentDefinition.name}`, async () => {
      const component = await createComponent(componentDefinition.name);
      component!.focus();
      await pickComponent(component!);
      const overlay = document.getElementsByTagName(componentDefinition.overlayTagName).item(0);
      expect(overlay).not.null;
      expect(overlay).to.be.exist;
    }).timeout(5000);

    it(`Overlay must be hidden when hide overlay method is called for the ${componentDefinition.name}`, async () => {
      const component = await createComponent(componentDefinition.name);
      await pickComponent(component!);
      const overlay = document.getElementsByTagName(componentDefinition.overlayTagName).item(0);
      componentOverlayManager.hideOverlay();
      //waiting overlay to be hidden.
      const openedAttribute = overlay!.hasAttribute('opened');
      expect(openedAttribute).to.be.false;
      componentOverlayManager.hideOverlay();
    }).timeout(5000);

    it(`Expanding editor must show/hide the overlay for the ${componentDefinition.name}`, async () => {
      const component = await createComponent(componentDefinition.name);
      await pickComponent(component!);
      editor.expanded = false;
      await elementUpdated(editor);
      const overlay = document.getElementsByTagName(componentDefinition.overlayTagName).item(0);
      //waiting overlay to be hidden.
      const openedAttribute = overlay!.hasAttribute('opened');
      expect(openedAttribute).to.be.false;
      editor.expanded = true;
      await elementUpdated(editor);
      await aTimeout(100);
      expect(overlay!.hasAttribute('opened')).to.be.true;
      componentOverlayManager.hideOverlay();
    }).timeout(5000);

    it(`Overlay must be visible when clicking on the theme editor panel ${componentDefinition.name}`, async () => {
      const component = await createComponent(componentDefinition.name);
      await pickComponent(component!);
      let element = editor.shadowRoot!.querySelector('vaadin-dev-tools-theme-property-list') as HTMLElement;
      element.click();
      const overlay = document.getElementsByTagName(componentDefinition.overlayTagName).item(0);
      //waiting overlay to be hidden.
      expect(overlay).not.null;
      expect(overlay).to.be.exist;
      await aTimeout(10);
      expect(overlay!.hasAttribute('opened')).to.be.true;
    }).timeout(5000);
    it(`Clicking on document should hide the overlay for ${componentDefinition.name}`, async () => {
      const component = await createComponent(componentDefinition.name);
      await pickComponent(component!);

      const overlay = document.getElementsByTagName(componentDefinition.overlayTagName).item(0);
      //waiting overlay to be hidden.
      expect(overlay).not.null;
      expect(overlay).to.be.exist;

      document.documentElement.click();
      await aTimeout(10);
      expect(overlay!.hasAttribute('opened')).to.be.false;
    }).timeout(5000);
  });

  async function createComponent(name: string) {
    const componentDefinition = componentsWithOverlay.find((p) => p.name === name);
    if (!componentDefinition) {
      return null;
    }
    await componentDefinition.import;
    await aTimeout(50);
    let element = (await fixture(componentDefinition.html)) as HTMLElement;
    currentComponentReference = { nodeId: 1234, uiId: 1234, element: element };
    apiMock.loadComponentMetadata.returns(Promise.resolve({ accessible: true, ...componentDefinition.metaData }));
    await elementUpdated(element);
    return element;
  }
});
