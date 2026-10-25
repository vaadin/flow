import { expect } from '@open-wc/testing';

// API under test — importing registers window.Vaadin.Flow.shortcut.
import '../../main/frontend/Shortcut';

const $wnd = window as any;

describe('shortcut origin guards', () => {
  let owner: HTMLElement;
  let popover: HTMLElement;
  let inPopover: HTMLElement;

  beforeEach(() => {
    owner = document.createElement('div');
    owner.setAttribute('data-shortcut-owner', 'owner');
    popover = document.createElement('div');
    popover.setAttribute('popover', 'manual');
    inPopover = document.createElement('input');
    popover.appendChild(inPopover);
    owner.appendChild(popover);
    document.body.appendChild(owner);
    popover.showPopover();
  });

  afterEach(() => {
    owner.remove();
  });

  // Dispatches a keydown on the target and returns what the guard made of it.
  function guardResult(target: EventTarget, guard: (event: Event) => boolean): boolean {
    let result: boolean | undefined;
    const listener = (event: Event) => {
      result = guard(event);
    };
    document.addEventListener('keydown', listener);
    target.dispatchEvent(new KeyboardEvent('keydown', { key: 'a', bubbles: true, composed: true }));
    document.removeEventListener('keydown', listener);
    return result!;
  }

  it('suppresses an event from an open popover inside the boundary', () => {
    const fromPopover = guardResult(inPopover, (e) => $wnd.Vaadin.Flow.shortcut.eventWithinBoundary(e, owner));
    const fromOwner = guardResult(owner, (e) => $wnd.Vaadin.Flow.shortcut.eventWithinBoundary(e, owner));
    expect(fromPopover).to.be.false;
    expect(fromOwner).to.be.true;
  });

  it('fires only for events in the scope of the owner', () => {
    const ownerSelector = '[data-shortcut-owner~="owner"]';
    const inScope = (e: Event) => $wnd.Vaadin.Flow.shortcut.eventInOwnerScope(e, ownerSelector);
    expect(guardResult(owner, inScope)).to.be.true;
    expect(guardResult(inPopover, inScope)).to.be.false;

    popover.removeChild(inPopover);
    popover.setAttribute('data-shortcut-owner', 'owner');
    owner.removeAttribute('data-shortcut-owner');
    popover.appendChild(inPopover);
    expect(guardResult(inPopover, inScope)).to.be.true;
  });

  it('fires a top-level shortcut only outside open popovers', () => {
    const topLevel = (e: Event) => $wnd.Vaadin.Flow.shortcut.eventInTopLevelScope(e);
    expect(guardResult(owner, topLevel)).to.be.true;
    expect(guardResult(inPopover, topLevel)).to.be.false;
  });

  it('relays a matching keydown to listenOn with the origin popover as its scope', () => {
    const listenOn = document.createElement('div');
    document.body.appendChild(listenOn);
    const relayed: Event[] = [];
    listenOn.addEventListener('keydown', (e) => relayed.push(e));
    $wnd.Vaadin.Flow.shortcut.registerKeydownDelegate(
      listenOn,
      popover,
      (e: KeyboardEvent) => e.key === 'a',
      false,
      false
    );

    const matching = new KeyboardEvent('keydown', { key: 'a', bubbles: true, cancelable: true });
    inPopover.dispatchEvent(matching);
    inPopover.dispatchEvent(new KeyboardEvent('keydown', { key: 'b', bubbles: true }));

    expect(relayed).to.have.length(1);
    expect((relayed[0] as KeyboardEvent).key).to.equal('a');
    expect(matching.defaultPrevented).to.be.true;
    // The relayed clone keeps the popover scope, so an owner outside of it
    // does not fire for an event that came from the popover.
    expect($wnd.Vaadin.Flow.shortcut.eventInTopLevelScope(relayed[0])).to.be.false;
    listenOn.remove();
  });
});
