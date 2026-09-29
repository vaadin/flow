import { expect } from '@open-wc/testing';

// API under test — importing registers window.Vaadin.Flow.getBrowserDetailsParameters.
import { collectBrowserDetails } from '../../main/frontend/BrowserHelpers';

const $wnd = window as any;

describe('BrowserHelpers', () => {
  it('installs collectBrowserDetails as getBrowserDetailsParameters', () => {
    expect($wnd.Vaadin.Flow.getBrowserDetailsParameters).to.equal(collectBrowserDetails);
  });

  it('collects the browser details as string parameters', async () => {
    const params = await $wnd.Vaadin.Flow.getBrowserDetailsParameters();

    expect(params['v-sw']).to.equal(String(window.screen.width));
    expect(params['v-tzid']).to.equal(Intl.DateTimeFormat().resolvedOptions().timeZone);
    expect(params).to.include.keys('v-pv', 'v-fs', 'v-so', 'v-soa', 'v-ga', 'v-wla', 'v-ws');
    Object.values(params).forEach((value) => expect(value).to.be.a('string'));
  });
});
