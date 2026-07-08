import { expect } from '@open-wc/testing';
import {
  BindingContext,
  bindDomEventListeners
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_LISTENERS = 4;

// Builds a BindingContext over a real DOM node with a fake state node:
//  - listeners: maps an event type to its constant-pool key
//  - constants: maps a constant-pool key to its expression settings object
//  - properties: ELEMENT_PROPERTIES property fakes keyed by name
function makeContext(
  htmlNode: Node,
  config: { listeners: Record<string, string>; constants: Record<string, unknown>; properties?: Record<string, any> }
) {
  const sent: Array<{ type: string; data: unknown }> = [];

  const listenerProps = Object.entries(config.listeners).map(([type, key]) => ({
    getName: () => type,
    hasValue: () => true,
    getValue: () => key,
    getSyncToServerCommand: () => () => {},
    setPreviousDomValue: () => {}
  }));
  const listenersMap = {
    getProperty: (type: string) => listenerProps.find((p) => p.getName() === type),
    forEachProperty: (cb: (property: any, name: string) => void) => listenerProps.forEach((p) => cb(p, p.getName())),
    addPropertyAddListener: () => ({ remove: () => {} })
  };
  const propertiesMap = {
    getProperty: (name: string) => (config.properties ?? {})[name],
    forEachProperty: () => {},
    addPropertyAddListener: () => ({ remove: () => {} })
  };
  const tree = {
    getRegistry: () => ({
      getConstantPool: () => ({ has: (k: string) => k in config.constants, get: (k: string) => config.constants[k] })
    }),
    sendEventToServer: (_node: unknown, type: string, data: unknown) => sent.push({ type, data }),
    getStateNodeForDomNode: () => null
  };
  const node: any = {
    getId: () => 1,
    getDomNode: () => htmlNode,
    getMap: (feature: number) => (feature === ELEMENT_LISTENERS ? listenersMap : propertiesMap),
    getList: () => ({ forEach: () => {} }),
    getTree: () => tree
  };
  const binderContext: any = { createAndBind: () => htmlNode, bind: () => {}, getStrategies: () => [] };
  return { context: new BindingContext(node, htmlNode, binderContext), sent };
}

describe('SimpleElementBindingStrategy DOM event listeners', () => {
  it('dispatches the collected event data to the server', () => {
    const element = document.createElement('div');
    const { context, sent } = makeContext(element, {
      listeners: { click: 'k1' },
      constants: { k1: { 'event.detail': false } }
    });
    bindDomEventListeners(context);

    element.dispatchEvent(new CustomEvent('click', { detail: 42 }));
    expect(sent).to.deep.equal([{ type: 'click', data: { 'event.detail': 42 } }]);
  });

  it('does not send when a boolean filter does not match, sends when it does', () => {
    const element = document.createElement('div');
    const { context, sent } = makeContext(element, {
      listeners: { click: 'k1' },
      constants: { k1: { 'event.altKey': true } }
    });
    bindDomEventListeners(context);

    element.dispatchEvent(new MouseEvent('click', { altKey: false }));
    expect(sent).to.deep.equal([]);

    element.dispatchEvent(new MouseEvent('click', { altKey: true }));
    expect(sent).to.have.length(1);
  });

  it('synchronizes a property and runs its sync command before sending', () => {
    const input = document.createElement('input');
    input.value = 'x';
    const setPrev: unknown[] = [];
    const synced: unknown[] = [];
    const valueProperty = {
      setPreviousDomValue: (v: unknown) => setPrev.push(v),
      getSyncToServerCommand: (v: unknown) => () => synced.push(v)
    };
    const { context, sent } = makeContext(input, {
      listeners: { input: 'k1' },
      constants: { k1: { '}value': false } },
      properties: { value: valueProperty }
    });
    bindDomEventListeners(context);

    input.dispatchEvent(new Event('input'));
    expect(setPrev).to.deep.equal(['x']);
    expect(synced).to.deep.equal(['x']);
    expect(sent).to.deep.equal([{ type: 'input', data: {} }]);
  });

  it('adds a DOM listener for the bound handler property', () => {
    const element = document.createElement('div');
    const { context, sent } = makeContext(element, {
      listeners: { click: 'k1' },
      constants: { k1: {} }
    });
    bindDomEventListeners(context);
    expect(context.listenerRemovers.has('click')).to.be.true;

    // An event with no expression settings is still sent (no filters).
    element.dispatchEvent(new MouseEvent('click'));
    expect(sent).to.deep.equal([{ type: 'click', data: null }]);
  });
});
