import { expect } from '@open-wc/testing';
import { bindPolymerModelProperties } from '../../main/frontend/internal/SimpleElementBindingStrategy';

describe('SimpleElementBindingStrategy', () => {
  function makeCallbacks() {
    const calls: Array<[string, unknown[]]> = [];
    return {
      calls,
      callbacks: {
        handlePropertiesChanged: (changedProps: unknown) => calls.push(['handlePropertiesChanged', [changedProps]]),
        fireReadyEvent: (element: unknown) => calls.push(['fireReadyEvent', [element]]),
        handleListItemPropertyChange: (nodeId: unknown, host: unknown, propertyName: string, value: unknown) =>
          calls.push(['handleListItemPropertyChange', [nodeId, host, propertyName, value]])
      }
    };
  }

  it('does nothing for a plain, non-custom element', () => {
    const { calls, callbacks } = makeCallbacks();
    const element = document.createElement('div');
    bindPolymerModelProperties(element as never, callbacks);
    expect(calls).to.deep.equal([]);
  });

  it('wraps _propertiesChanged so the original still runs after the callback', () => {
    const { calls, callbacks } = makeCallbacks();
    const originalCalls: unknown[][] = [];
    const element: Record<string, unknown> = {
      constructor: { polymerElementVersion: '3.0' },
      root: null,
      _propertiesChanged(this: unknown, ...args: unknown[]) {
        originalCalls.push(args);
      },
      ready() {},
      addEventListener() {},
      removeEventListener() {}
    };

    bindPolymerModelProperties(element as never, callbacks);

    (element._propertiesChanged as (...a: unknown[]) => void)('current', 'changed', 'old');
    // The callback fires with the changed-properties argument, then the original.
    expect(calls).to.deep.equal([['handlePropertiesChanged', ['changed']]]);
    expect(originalCalls).to.deep.equal([['current', 'changed', 'old']]);
  });

  it('wraps ready to fire the ready event and register a dom-change listener when no dom-repeat exists', () => {
    const { calls, callbacks } = makeCallbacks();
    const readyCalls: string[] = [];
    const listeners: Array<[string, unknown]> = [];
    const element: Record<string, unknown> = {
      constructor: { polymerElementVersion: '3.0' },
      root: null,
      _propertiesChanged() {},
      ready() {
        readyCalls.push('original');
      },
      addEventListener(type: string, listener: unknown) {
        listeners.push([type, listener]);
      },
      removeEventListener() {}
    };

    bindPolymerModelProperties(element as never, callbacks);
    (element.ready as () => void)();

    // The original ready runs first, then the ready event fires for the element.
    expect(readyCalls).to.deep.equal(['original']);
    expect(calls).to.deep.equal([['fireReadyEvent', [element]]]);
    // With no dom-repeat in the (absent) shadow root, a dom-change listener is registered.
    expect(listeners.map((l) => l[0])).to.deep.equal(['dom-change']);
  });
});
