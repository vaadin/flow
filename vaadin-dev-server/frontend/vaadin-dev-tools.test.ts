import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import { VaadinDevTools } from './vaadin-dev-tools';
import './vaadin-dev-tools';
import { ThemeEditorState } from './theme-editor/model';

describe('vaadin-dev-tools', function () {
  let devTools: VaadinDevTools;

  beforeEach(async () => {
    // Reset global state so that dev tools don't choke when being created multiple times
    // @ts-ignore
    window.Vaadin = {};
    // @ts-ignore
    window.Vaadin.Flow = {};
    // @ts-ignore
    window.Vaadin.Flow.clients = {};
    // @ts-ignore
    window.Vaadin.ConsoleErrors = [];
    // @ts-ignore
    window.Vaadin.devTools = {};
    // @ts-ignore
    window.Vaadin.devTools.createdCvdlElements = [];
    window.sessionStorage.setItem(VaadinDevTools.ACTIVE_KEY_IN_SESSION_STORAGE, 'true');
    devTools = await fixture(html` <vaadin-dev-tools></vaadin-dev-tools>`);
  });

  describe('tabs', () => {
    function getTab(tabId: string) {
      return devTools.shadowRoot!.querySelector(`button.tab#${tabId}`) as HTMLElement;
    }

    function sendThemeEditorStateMessage(state: ThemeEditorState) {
      const message = {
        command: 'themeEditorState',
        data: state
      };
      devTools.handleFrontendMessage(message);
    }

    it('should not show theme editor tab by default', () => {
      const themeEditorTab = getTab('theme-editor');
      expect(themeEditorTab).to.be.null;
      // Sanity check that any tab is rendered at this point
      const logTab = getTab('log');
      expect(logTab).to.not.be.null;
    });

    it('should not show theme editor tab when it is in disabled state', async () => {
      sendThemeEditorStateMessage(ThemeEditorState.disabled);
      await elementUpdated(devTools);

      const themeEditorTab = getTab('theme-editor');
      expect(themeEditorTab).to.be.null;
    });

    it('should show theme editor tab when it is in enabled state', async () => {
      sendThemeEditorStateMessage(ThemeEditorState.enabled);
      await elementUpdated(devTools);

      const themeEditorTab = getTab('theme-editor');
      expect(themeEditorTab).to.not.be.null;
    });

    it('should show theme editor tab when it is in missing theme state', async () => {
      sendThemeEditorStateMessage(ThemeEditorState.missing_theme);
      await elementUpdated(devTools);

      const themeEditorTab = getTab('theme-editor');
      expect(themeEditorTab).to.not.be.null;
    });
  });
});
