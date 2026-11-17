import { expect } from '@open-wc/testing';

// API under test — importing registers window.Vaadin.Flow.getBrowserDetailsParameters.
import { collectBrowserDetails } from '../../main/frontend/BrowserHelpers';
import { initWindowName } from '../../main/frontend/WindowName';

const $wnd = window as any;

describe('BrowserHelpers', () => {
  it('installs collectBrowserDetails as getBrowserDetailsParameters', () => {
    expect($wnd.Vaadin.Flow.getBrowserDetailsParameters).to.equal(collectBrowserDetails);
  });

  it('installs the shortcut and web push helpers', () => {
    expect($wnd.Vaadin.Flow.shortcut.eventInOwnerScope).to.be.a('function');
    expect($wnd.Vaadin.Flow.webPush.subscribe).to.be.a('function');
  });

  it('collects the browser details as string parameters', async () => {
    const params = await $wnd.Vaadin.Flow.getBrowserDetailsParameters();

    expect(params['v-sw']).to.equal(String(window.screen.width));
    expect(params['v-tzid']).to.equal(Intl.DateTimeFormat().resolvedOptions().timeZone);
    expect(params).to.include.keys('v-pv', 'v-fs', 'v-so', 'v-soa', 'v-ga', 'v-wla', 'v-ws');
    Object.values(params).forEach((value) => expect(value).to.be.a('string'));
  });

  it('sends the window name once it has been resolved', async () => {
    const originalName = window.name;
    window.name = '';
    sessionStorage.setItem('vaadin.windowName', 'returning-tab');
    try {
      void initWindowName();
      const params = await collectBrowserDetails();

      expect(params['v-wn']).to.equal('returning-tab');
    } finally {
      window.name = originalName;
      sessionStorage.removeItem('vaadin.windowName');
    }
  });
});
