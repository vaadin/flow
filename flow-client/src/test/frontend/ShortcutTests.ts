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
    // An owner that is not in the document does not block the shortcut.
    const missingOwner = (e: Event) =>
      $wnd.Vaadin.Flow.shortcut.eventInOwnerScope(e, '[data-shortcut-owner~="missing"]');
    expect(guardResult(inPopover, missingOwner)).to.be.true;

    popover.removeChild(inPopover);
    popover.setAttribute('data-shortcut-owner', 'owner');
    owner.removeAttribute('data-shortcut-owner');
    popover.appendChild(inPopover);
    expect(guardResult(inPopover, inScope)).to.be.true;
  });

  it('resolves the scope of a slotted owner through the shadow tree', () => {
    const ownerSelector = '[data-shortcut-owner~="slotted"]';
    const inScope = (e: Event) => $wnd.Vaadin.Flow.shortcut.eventInOwnerScope(e, ownerSelector);

    // The open popover is in the shadow root of a component and the owner is
    // slotted into it, like the content of an overlay component.
    const overlayHost = document.createElement('div');
    const shadowPopover = document.createElement('div');
    shadowPopover.setAttribute('popover', 'manual');
    shadowPopover.appendChild(document.createElement('slot'));
    overlayHost.attachShadow({ mode: 'open' }).appendChild(shadowPopover);
    const slottedOwner = document.createElement('input');
    slottedOwner.setAttribute('data-shortcut-owner', 'slotted');
    overlayHost.appendChild(slottedOwner);
    document.body.appendChild(overlayHost);
    shadowPopover.showPopover();
    try {
      expect(guardResult(slottedOwner, inScope)).to.be.true;
      expect(guardResult(document.body, inScope)).to.be.false;
    } finally {
      overlayHost.remove();
    }

    // The owner is slotted into a component that sits inside an open popover,
    // so its scope is found by leaving the shadow root through its host.
    const host = document.createElement('div');
    host.attachShadow({ mode: 'open' }).appendChild(document.createElement('slot'));
    const ownerInHost = document.createElement('span');
    ownerInHost.setAttribute('data-shortcut-owner', 'slotted');
    host.appendChild(ownerInHost);
    popover.appendChild(host);
    expect(guardResult(inPopover, inScope)).to.be.true;
    expect(guardResult(owner, inScope)).to.be.false;
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
    const options = {
      keys: ['KeyA'],
      requiredModifiers: 'Control',
      forbiddenModifiers: 'Shift',
      checkBoundary: true,
      resetFocus: false,
      allowDefault: false
    };
    // The locator is called with listenOn as this, like a component's
    // locator that returns an overlay it keeps a reference to.
    (listenOn as any).overlay = owner;
    $wnd.Vaadin.Flow.shortcut.registerKeydownDelegate(
      listenOn,
      function (this: any) {
        return this.overlay;
      },
      options
    );

    const matching = new KeyboardEvent('keydown', {
      code: 'KeyA',
      key: 'a',
      ctrlKey: true,
      bubbles: true,
      cancelable: true
    });
    owner.dispatchEvent(matching);
    owner.dispatchEvent(new KeyboardEvent('keydown', { code: 'KeyB', ctrlKey: true, bubbles: true }));
    owner.dispatchEvent(new KeyboardEvent('keydown', { code: 'KeyA', ctrlKey: true, shiftKey: true, bubbles: true }));
    // An open popover inside the delegate element blocks the relay.
    inPopover.dispatchEvent(new KeyboardEvent('keydown', { code: 'KeyA', ctrlKey: true, bubbles: true }));

    expect(relayed).to.have.length(1);
    expect((relayed[0] as KeyboardEvent).code).to.equal('KeyA');
    expect(matching.defaultPrevented).to.be.true;
    expect(() => $wnd.Vaadin.Flow.shortcut.registerKeydownDelegate(listenOn, () => null, options)).to.throw(
      /not found/
    );
    listenOn.remove();
  });

  it('keeps the origin popover as the scope of a relayed keydown', () => {
    const listenOn = document.createElement('div');
    document.body.appendChild(listenOn);
    const relayed: Event[] = [];
    listenOn.addEventListener('keydown', (e) => relayed.push(e));
    $wnd.Vaadin.Flow.shortcut.registerKeydownDelegate(listenOn, () => popover, {
      keys: ['a'],
      requiredModifiers: '',
      forbiddenModifiers: '',
      checkBoundary: false,
      resetFocus: false,
      allowDefault: true
    });

    const matching = new KeyboardEvent('keydown', { key: 'a', bubbles: true, cancelable: true });
    inPopover.dispatchEvent(matching);

    expect(relayed).to.have.length(1);
    expect(matching.defaultPrevented).to.be.false;
    // The relayed clone keeps the popover scope, so an owner outside of it
    // does not fire for an event that came from the popover.
    expect($wnd.Vaadin.Flow.shortcut.eventInTopLevelScope(relayed[0])).to.be.false;
    listenOn.remove();
  });

  it('passes a keydown that is the shortcut and applies the side effects', () => {
    const options = {
      keys: ['Digit1', '1'],
      requiredModifiers: 'Alt',
      forbiddenModifiers: 'Shift Control Meta',
      checkOwnerScope: false,
      ownerSelector: null,
      preventDefault: true,
      stopPropagation: true,
      resetFocus: false
    };
    const keydown = (init: KeyboardEventInit) => new KeyboardEvent('keydown', { cancelable: true, ...init });
    const matchKeydown = (event: KeyboardEvent, overrides = {}) =>
      $wnd.Vaadin.Flow.shortcut.matchKeydown(event, { ...options, ...overrides });

    // Matched by key code or by key value
    const byCode = keydown({ code: 'Digit1', key: '!', altKey: true });
    expect(matchKeydown(byCode)).to.be.true;
    expect(byCode.defaultPrevented).to.be.true;
    expect(matchKeydown(keydown({ code: 'Numpad1', key: '1', altKey: true }))).to.be.true;

    const missingModifier = keydown({ code: 'Digit1' });
    expect(matchKeydown(missingModifier)).to.be.false;
    // A keydown that is not the shortcut is left untouched
    expect(missingModifier.defaultPrevented).to.be.false;
    expect(matchKeydown(keydown({ code: 'Digit1', altKey: true, shiftKey: true }))).to.be.false;
    expect(matchKeydown(keydown({ code: 'Digit2', altKey: true }))).to.be.false;

    const allowDefault = keydown({ code: 'Digit1', altKey: true });
    expect(matchKeydown(allowDefault, { preventDefault: false })).to.be.true;
    expect(allowDefault.defaultPrevented).to.be.false;
  });

  it('passes a keydown only from the scope of the owner when asked to', () => {
    const options = {
      keys: ['a'],
      requiredModifiers: '',
      forbiddenModifiers: '',
      checkOwnerScope: true,
      ownerSelector: '[data-shortcut-owner~="owner"]',
      preventDefault: true,
      stopPropagation: false,
      resetFocus: false
    };
    const filter =
      (overrides = {}) =>
      (e: Event) =>
        $wnd.Vaadin.Flow.shortcut.matchKeydown(e, { ...options, ...overrides });

    expect(guardResult(owner, filter())).to.be.true;
    expect(guardResult(inPopover, filter())).to.be.false;
    expect(guardResult(inPopover, filter({ ownerSelector: null }))).to.be.false;
    expect(guardResult(owner, filter({ ownerSelector: null }))).to.be.true;
    expect(guardResult(inPopover, filter({ checkOwnerScope: false }))).to.be.true;
  });
});
