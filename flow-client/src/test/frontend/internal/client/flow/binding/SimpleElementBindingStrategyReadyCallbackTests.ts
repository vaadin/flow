import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { addReadyListener } from '../../../../../../main/frontend/internal/client/PolymerUtils';
import { NodeFeatures, NodeProperties, StateNode, bind, makeCollectingTree } from '../bindingTestHelpers';

// Ported from the testReadyCallback_* cases in GwtBasicElementBinderTest. Binding
// a Polymer element wraps its `ready` method so that the element's own `ready`
// still runs and any registered ready listeners are notified when it fires. The
// deferred cases cover an element that only becomes a Polymer element after
// customElements.whenDefined resolves.
describe('SimpleElementBindingStrategy ready callback', () => {
  let savedPolymer: unknown;

  beforeEach(() => {
    Reactive.reset();
    savedPolymer = (window as unknown as { Polymer?: unknown }).Polymer;
  });

  afterEach(() => {
    (window as unknown as { Polymer?: unknown }).Polymer = savedPolymer;
    // Drop any whenDefined override installed by mockWhenDefined, restoring the
    // native CustomElementRegistry.whenDefined from the prototype.
    delete (window.customElements as unknown as { whenDefined?: unknown }).whenDefined;
    Reactive.flush();
  });

  // Makes `element` pass PolymerUtils.isPolymerElement by installing a
  // window.Polymer function whose Element prototype is spliced into the element's
  // prototype chain: the element keeps its DOM methods but is `instanceof
  // Polymer.Element`. Mirrors GwtBasicElementBinderTest.initPolymer.
  function initPolymer(element: Element): void {
    const polymer = function (): void {} as unknown as {
      (): void;
      dom: (node: unknown) => unknown;
      Element: new () => unknown;
    };
    polymer.dom = (node: unknown) => node;
    const PolymerElement = function (): void {} as unknown as new () => unknown;
    polymer.Element = PolymerElement;
    Object.setPrototypeOf(PolymerElement.prototype, Object.getPrototypeOf(element));
    Object.setPrototypeOf(element, PolymerElement.prototype);
    (window as unknown as { Polymer?: unknown }).Polymer = polymer;
  }

  // Replaces customElements.whenDefined with a promise this test controls, and
  // hides Polymer so the element is only "may be a polymer element" during
  // binding. Mirrors GwtBasicElementBinderTest.mockWhenDefined. runWhenDefined
  // restores Polymer and resolves the promise.
  function mockWhenDefined(): { runWhenDefined: () => void } {
    const oldPolymer = (window as unknown as { Polymer?: unknown }).Polymer;
    (window as unknown as { Polymer?: unknown }).Polymer = null;
    let resolvePromise: () => void = () => {};
    (window.customElements as unknown as { whenDefined: () => Promise<unknown> }).whenDefined = () =>
      new Promise<unknown>((resolve) => {
        resolvePromise = () => resolve(undefined);
      });
    return {
      runWhenDefined: () => {
        (window as unknown as { Polymer?: unknown }).Polymer = oldPolymer;
        resolvePromise();
      }
    };
  }

  function bindNode(element: Element): void {
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.setDomNode(element);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(element.tagName.toLowerCase());
    bind(node, element);
  }

  // Mirrors GwtBasicElementBinderTest.assertPolymerElement_originalReadyIsCalled.
  function assertPolymerElementOriginalReadyIsCalled(element: Element): void {
    initPolymer(element);

    (element as unknown as { ready: () => void }).ready = function (this: Record<string, unknown>): void {
      this.foo = 'bar';
    };

    bindNode(element);

    (element as unknown as { ready: () => void }).ready();

    expect((element as unknown as Record<string, unknown>).foo).to.equal('bar');
  }

  // Mirrors GwtBasicElementBinderTest.assertDeferredPolymerElement_originalReadyIsCalled.
  async function assertDeferredPolymerElementOriginalReadyIsCalled(element: Element): Promise<void> {
    initPolymer(element);
    const control = mockWhenDefined();

    (element as unknown as { ready: () => void }).ready = function (this: Record<string, unknown>): void {
      this.foo = 'bar';
    };

    bindNode(element);

    control.runWhenDefined();

    // Let the whenDefined promise (and the race in bindPolymerModelProperties)
    // settle so the element gets hooked up as a Polymer element.
    await new Promise((resolve) => setTimeout(resolve, 0));

    (element as unknown as { ready: () => void }).ready();

    expect((element as unknown as Record<string, unknown>).foo).to.equal('bar');
  }

  it('runs the element original ready after binding a polymer element', () => {
    // Ported from testReadyCallback_polymerElementAndNoListeners_readyIsCalled.
    const element = document.createElement('div');

    assertPolymerElementOriginalReadyIsCalled(element);
  });

  it('notifies ready listeners and still runs the original ready', () => {
    // Ported from testReadyCallback_polymerElement_readyIsCalledAndNotified.
    const element = document.createElement('div');

    addReadyListener(element, () => {
      (element as unknown as Record<string, unknown>).baz = 'foobar';
    });

    assertPolymerElementOriginalReadyIsCalled(element);

    expect((element as unknown as Record<string, unknown>).baz).to.equal('foobar');
  });

  it('runs the element original ready after a deferred polymer element is defined', async () => {
    // Ported from testReadyCallback_deferredPolymerElementAndNoListeners_readyIsCalled.
    const element = document.createElement('x-my');

    await assertDeferredPolymerElementOriginalReadyIsCalled(element);
  });

  it('notifies ready listeners after a deferred polymer element is defined', async () => {
    // Ported from testReadyCallback_deferredPolymerElement_readyIsCalledAndNotified.
    const element = document.createElement('x-my');

    addReadyListener(element, () => {
      (element as unknown as Record<string, unknown>).baz = 'foobar';
    });

    await assertDeferredPolymerElementOriginalReadyIsCalled(element);

    expect((element as unknown as Record<string, unknown>).baz).to.equal('foobar');
  });
});
