import { expect } from '@open-wc/testing';

// API to test
import { initBrowserTabIdentifier } from '../../main/frontend/BrowserTabIdentifier';

const STORAGE_KEY = 'vaadin.windowName';
const CHANNEL_NAME = 'vaadin-window-name';

describe('BrowserTabIdentifier', () => {
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

  // Runs the code under test in a same-origin frame, which shares
  // sessionStorage with this page. Returns the name stored before the code ran,
  // as seen by the frame, and the frame's resolved name.
  async function initInFrame(): Promise<{ storedName: string | null; name: string }> {
    const moduleUrl = new URL('../../main/frontend/BrowserTabIdentifier.ts', import.meta.url).href;
    const frame = document.createElement('iframe');
    frame.srcdoc = `<script>
      const storedName = sessionStorage.getItem('${STORAGE_KEY}');
      window.result = import('${moduleUrl}')
        .then((module) => module.initBrowserTabIdentifier())
        .then(() => ({ storedName, name: window.name }));</script>`;
    const loaded = new Promise((resolve) => {
      frame.onload = resolve;
    });
    document.body.appendChild(frame);
    try {
      await loaded;
      return await (frame.contentWindow as unknown as { result: Promise<{ storedName: string | null; name: string }> })
        .result;
    } finally {
      frame.remove();
    }
  }

  it('neither takes back nor stores a name in a frame', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'top-tab');

    const frame = await initInFrame();

    expect(frame.storedName).to.equal('top-tab');
    expect(frame.name).to.match(/^v-0\.\d+/);
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal('top-tab');
  });

  it('keeps a window name that is already set and stores it', async () => {
    window.name = 'named-tab';
    sessionStorage.setItem(STORAGE_KEY, 'copied-from-opener');

    await initBrowserTabIdentifier();

    expect(window.name).to.equal('named-tab');
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal('named-tab');
  });

  it('generates and stores a window name for a new tab', async () => {
    await initBrowserTabIdentifier();

    expect(window.name).to.match(/^v-0\.\d+/);
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal(window.name);
  });

  it('takes the stored name back when no other page uses it', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'returning-tab');
    openOtherPageUsing('some-other-tab');

    await initBrowserTabIdentifier();

    expect(window.name).to.equal('returning-tab');
  });

  it('generates a new name when another page uses the stored one', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'original-tab');
    openOtherPageUsing('original-tab');

    await initBrowserTabIdentifier();

    expect(window.name).to.match(/^v-0\.\d+/);
    expect(sessionStorage.getItem(STORAGE_KEY)).to.equal(window.name);
  });

  it('answers only for its own window name', async () => {
    window.name = 'live-tab';
    await initBrowserTabIdentifier();

    const answers = await queryFromOtherPage('other-tab', 'live-tab');

    expect(answers).to.deep.equal([{ type: 'in-use', name: 'live-tab' }]);
  });

  it('answers for the stored name while taking it back', async () => {
    sessionStorage.setItem(STORAGE_KEY, 'returning-tab');

    const ready = initBrowserTabIdentifier();
    const answers = await queryFromOtherPage('returning-tab');
    await ready;

    expect(answers).to.deep.equal([{ type: 'in-use', name: 'returning-tab' }]);
    expect(window.name).to.equal('returning-tab');
  });
});
