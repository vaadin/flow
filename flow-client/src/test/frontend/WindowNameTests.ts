import { expect } from '@open-wc/testing';

import { collectBrowserDetails } from '../../main/frontend/BrowserHelpers';
// API to test
import { initWindowName } from '../../main/frontend/WindowName';

const STORAGE_KEY = 'vaadin.windowName';
const CHANNEL_NAME = 'vaadin-window-name';

describe('WindowName', () => {
  const originalName = window.name;
  let otherPage: BroadcastChannel | undefined;

  beforeEach(() => {
    window.name = '';
    sessionStorage.removeItem(STORAGE_KEY);
  });

  afterEach(() => {
    otherPage?.close();
    otherPage = undefined;
    window.name = originalName;
    sessionStorage.removeItem(STORAGE_KEY);
  });

  // Stands in for another open page that uses the given window name
  function openOtherPageUsing(name: string) {
    otherPage = new BroadcastChannel(CHANNEL_NAME);
    otherPage.onmessage = (event) => {
      if (event.data.type === 'query' && event.data.name === name) {
        otherPage!.postMessage({ type: 'in-use', name });
      }
    };
  }

  it('keeps a window name that is already set and stores it', async () => {
    window.name = 'named-tab';
    sessionStorage.setItem(STORAGE_KEY, 'copied-from-opener');

    await initWindowName();

    expect(window.name).to.equal('named-tab');
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal('named-tab');
  });

  it('generates and stores a window name for a new tab', async () => {
    await initWindowName();

    expect(window.name).to.match(/^v-0\.\d+/);
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal(window.name);
  });

  it('takes the stored name back when no other page uses it', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'returning-tab');
    openOtherPageUsing('some-other-tab');

    void initWindowName();
    const params = await collectBrowserDetails();

    expect(window.name).to.equal('returning-tab');
    expect(params['v-wn']).to.equal('returning-tab');
  });

  it('generates a new name when another page uses the stored one', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'original-tab');
    openOtherPageUsing('original-tab');

    await initWindowName();

    expect(window.name).to.match(/^v-0\.\d+/);
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal(window.name);
  });

  it('answers that its window name is in use', async () => {
    window.name = 'live-tab';
    await initWindowName();

    otherPage = new BroadcastChannel(CHANNEL_NAME);
    const answer = new Promise<unknown>((resolve) => {
      otherPage!.onmessage = (event) => resolve(event.data);
    });
    otherPage.postMessage({ type: 'query', name: 'live-tab' });

    expect(await answer).to.deep.equal({ type: 'in-use', name: 'live-tab' });
  });
});
