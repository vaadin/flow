import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { bindVisibility, BindingContext } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

// A stateful MapProperty stand-in.
function fakeProperty(initial?: unknown) {
  let value = initial;
  let has = arguments.length > 0;
  const listeners: Array<() => void> = [];
  return {
    listeners,
    getName: () => 'p',
    hasValue: () => has,
    getValue: () => value,
    setValue: (v: unknown) => {
      value = v;
      has = true;
    },
    addChangeListener: (l: () => void) => {
      listeners.push(l);
      return { remove: () => listeners.splice(listeners.indexOf(l), 1) };
    }
  };
}

const configuration = { isWebComponentMode: () => false, getServiceUrl: () => '' };

// A node whose ELEMENT_DATA map has visibility properties, and whose tree
// reports visibility.
function fakeNode(node: { visible: boolean }, props: Record<string, any>): any {
  const map = {
    getProperty: (name: string) => (props[name] ??= fakeProperty()),
    getNode: () => ({
      getTree: () => ({ getRegistry: () => ({ getApplicationConfiguration: () => configuration }) })
    })
  };
  return {
    getMap: (_feature: number) => map,
    getDomNode: () => null,
    setDomNode: () => {},
    getTree: () => ({ isVisible: () => node.visible })
  };
}

const noopFactory: any = { createAndBind: () => {}, bind: () => {}, getStrategies: () => [] };

describe('SimpleElementBindingStrategy visibility binding', () => {
  afterEach(() => Reactive.flush());

  it('marks the bound property when visible', () => {
    const props: Record<string, any> = {};
    const node = fakeNode({ visible: true }, props);
    const element = document.createElement('div');
    bindVisibility([], new BindingContext(node, element, noopFactory), [], noopFactory);
    Reactive.flush();
    expect(props.bound.getValue()).to.equal(true);
  });

  it('hides the element when not visible', () => {
    const props: Record<string, any> = {};
    const node = fakeNode({ visible: false }, props);
    const element = document.createElement('div');
    bindVisibility([], new BindingContext(node, element, noopFactory), [], noopFactory);
    Reactive.flush();
    expect(element.getAttribute('hidden')).to.equal('true');
  });

  it('re-applies visibility when the VISIBLE property changes', () => {
    const props: Record<string, any> = {};
    const state = { visible: false };
    const node = fakeNode(state, props);
    const element = document.createElement('div');
    bindVisibility([], new BindingContext(node, element, noopFactory), [], noopFactory);
    Reactive.flush();
    expect(element.getAttribute('hidden')).to.equal('true');

    // Become visible and fire the VISIBLE change listener (triggers a rebind).
    state.visible = true;
    props.visible.listeners.forEach((l: () => void) => l());
    Reactive.flush();
    expect(element.hasAttribute('hidden')).to.be.false;
  });
});
