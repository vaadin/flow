import { expect } from '@open-wc/testing';

// API under test — importing registers window.Vaadin.Flow.fullscreen.
import '../../main/frontend/Fullscreen';

const $wnd = window as any;

// Replace document.documentElement.requestFullscreen with a resolving stub so
// the tests exercise the DOM manipulation without depending on a real
// fullscreen grant (which requires user activation and is denied in CI).
function stubRequestFullscreen(): () => void {
  const original = document.documentElement.requestFullscreen;
  document.documentElement.requestFullscreen = () => Promise.resolve();
  return () => {
    document.documentElement.requestFullscreen = original;
  };
}

describe('requestComponentFullscreen', () => {
  let restore: () => void;
  let wrapper: HTMLElement;

  beforeEach(() => {
    restore = stubRequestFullscreen();
    wrapper = document.createElement('flow-container-test');
    document.body.appendChild(wrapper);
  });

  afterEach(() => {
    restore();
    wrapper.remove();
    // Drop any active reset state captured by a previous test run.
    document.dispatchEvent(new Event('fullscreenchange'));
  });

  it('keeps the component visible when it is the view root (direct child of wrapper)', async () => {
    // The view root is the only child of the wrapper, and it is itself the
    // component being fullscreened. The placeholder ends up as the wrapper's
    // first node, so the old firstChild-based logic crashed on the comment.
    const viewRoot = document.createElement('div');
    wrapper.appendChild(viewRoot);

    await $wnd.Vaadin.Flow.fullscreen.requestComponentFullscreen(viewRoot, wrapper);

    expect(viewRoot.parentElement).to.equal(wrapper);
    // Must not be hidden — hiding the view root here would blank the screen.
    expect(viewRoot.style.display).to.not.equal('none');
  });

  it('hides the view root and restores it on exit for a nested component', async () => {
    const viewRoot = document.createElement('div');
    const panel = document.createElement('div');
    viewRoot.appendChild(panel);
    wrapper.appendChild(viewRoot);

    await $wnd.Vaadin.Flow.fullscreen.requestComponentFullscreen(panel, wrapper);

    // Panel moved into the wrapper, view root hidden, placeholder left behind.
    expect(panel.parentElement).to.equal(wrapper);
    expect(viewRoot.style.display).to.equal('none');
    expect(viewRoot.firstChild?.nodeType).to.equal(Node.COMMENT_NODE);

    // Exiting fullscreen restores the original DOM.
    document.dispatchEvent(new Event('fullscreenchange'));

    expect(panel.parentElement).to.equal(viewRoot);
    expect(viewRoot.style.display).to.not.equal('none');
    expect(viewRoot.firstChild).to.equal(panel);
  });
});
