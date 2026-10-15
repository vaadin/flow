import { expect } from '@open-wc/testing';

// API under test — importing registers window.Vaadin.Flow.download.
import '../../main/frontend/Download';

const $wnd = window as any;

describe('download.start', () => {
  let container: HTMLElement;
  let button: HTMLElement;
  let clickedAnchor: HTMLAnchorElement | undefined;
  let clickedAnchorParent: Node | null;
  let insideCaptureClicks: number;
  let buttonClicks: number;

  // Mirrors how overlays detect an outside click: a capture-phase listener on
  // the document that checks whether the click happened inside the overlay.
  function captureListener(e: Event) {
    if (e.composedPath().includes(container)) {
      insideCaptureClicks += 1;
    }
  }

  // Records the anchor while it is still in the document, then prevents the
  // default action so the test does not actually navigate or download.
  function anchorClickListener(e: Event) {
    if (e.target instanceof HTMLAnchorElement && e.target.hasAttribute('download')) {
      clickedAnchor = e.target;
      clickedAnchorParent = e.target.parentNode;
      e.preventDefault();
    }
  }

  beforeEach(() => {
    clickedAnchor = undefined;
    clickedAnchorParent = null;
    insideCaptureClicks = 0;
    buttonClicks = 0;
    container = document.createElement('div');
    button = document.createElement('button');
    container.appendChild(button);
    document.body.appendChild(container);
    document.documentElement.addEventListener('click', captureListener, true);
    window.addEventListener('click', anchorClickListener, true);
  });

  afterEach(() => {
    document.documentElement.removeEventListener('click', captureListener, true);
    window.removeEventListener('click', anchorClickListener, true);
    container.remove();
  });

  it('clicks the anchor next to the element that fired the event', () => {
    button.addEventListener('click', (e) => {
      buttonClicks += 1;
      $wnd.Vaadin.Flow.download.start(e, '/files/a.bin', 'b.bin');
    });

    button.click();

    expect(clickedAnchor).to.exist;
    expect(clickedAnchor!.getAttribute('href')).to.equal('/files/a.bin');
    expect(clickedAnchor!.download).to.equal('b.bin');
    expect(clickedAnchorParent).to.equal(container);
    // The overlay-style listener sees both clicks as inside clicks.
    expect(insideCaptureClicks).to.equal(2);
    // The anchor click does not bubble back into the trigger element.
    expect(buttonClicks).to.equal(1);
    expect(container.querySelector('a')).to.be.null;
  });

  it('falls back to the document body without an element target', () => {
    $wnd.Vaadin.Flow.download.start(undefined, '/files/a.bin');

    expect(clickedAnchor).to.exist;
    expect(clickedAnchor!.download).to.equal('');
    expect(clickedAnchorParent).to.equal(document.body);
    expect(document.body.querySelector('a[download]')).to.be.null;
  });
});
