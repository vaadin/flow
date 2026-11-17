import { expect } from '@open-wc/testing';

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

  // Stands in for another open page that answers every query with its own
  // window name
  function openOtherPageUsing(name: string) {
    otherPage = new BroadcastChannel(CHANNEL_NAME);
    otherPage.onmessage = (event) => {
      if (event.data.type === 'query') {
        otherPage!.postMessage({ type: 'in-use', name });
      }
    };
  }

  // Sends queries for the given names from another page and collects the
  // answers that arrive within the timeout of the code under test
  async function queryFromOtherPage(...names: string[]): Promise<unknown[]> {
    const answers: unknown[] = [];
    otherPage = new BroadcastChannel(CHANNEL_NAME);
    otherPage.onmessage = (event) => {
      if (event.data.type === 'in-use') {
        answers.push(event.data);
      }
    };
    names.forEach((name) => otherPage!.postMessage({ type: 'query', name }));
    await new Promise((resolve) => {
      setTimeout(resolve, 300);
    });
    return answers;
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

    await initWindowName();

    expect(window.name).to.equal('returning-tab');
  });

  it('generates a new name when another page uses the stored one', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'original-tab');
    openOtherPageUsing('original-tab');

    await initWindowName();

    expect(window.name).to.match(/^v-0\.\d+/);
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal(window.name);
  });

  it('answers only for its own window name', async () => {
    window.name = 'live-tab';
    await initWindowName();

    const answers = await queryFromOtherPage('other-tab', 'live-tab');

    expect(answers).to.deep.equal([{ type: 'in-use', name: 'live-tab' }]);
  });

  it('answers for the stored name while taking it back', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'returning-tab');

    const ready = initWindowName();
    const answers = await queryFromOtherPage('returning-tab');
    await ready;

    expect(answers).to.deep.equal([{ type: 'in-use', name: 'returning-tab' }]);
  });
});
