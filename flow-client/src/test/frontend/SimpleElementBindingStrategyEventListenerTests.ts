import { expect } from '@open-wc/testing';
import {
  BindingContext,
  bindDomEventListeners
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { Debouncer } from '../../main/frontend/internal/binding/Debouncer';
import { BindGuardStateNode, NodeFeatures, StateNode, bind, makeCollectingTree } from './bindingTestHelpers';

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

// Full-state-tree DOM-event tests ported from GwtPropertyElementBinderTest and
// GwtMultipleBindingTest. They bind a real StateNode to a real element attached
// to the document, wire an ELEMENT_LISTENERS listener via the constant pool, and
// dispatch a real DOM event.
describe('SimpleElementBindingStrategy DOM event listeners (full tree)', () => {
  const SYNCHRONIZE_PROPERTY_TOKEN = '}';
  const EVENT_PHASE_TRAILING = 'trailing';

  let harness: ReturnType<typeof makeCollectingTree>;
  let node: StateNode;
  let element: HTMLElement;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);
    element = document.createElement('div');
    document.body.appendChild(element);
  });

  afterEach(() => {
    element.remove();
    Reactive.flush();
  });

  function addListenerConstant(key: string, expressions: Record<string, unknown>): void {
    harness.constantPool.importFromJson({ [key]: expressions });
    node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty('event1').setValue(key);
  }

  it('synchronizes only the event-specific property, not globally-marked ones', () => {
    // Ported from testDomListenerSynchronization.
    bind(node, element);

    // Only offsetWidth is requested by the event's expression; offsetHeight is
    // not part of any event synchronization, so it must not be synced.
    addListenerConstant('expressionsKey', { [`${SYNCHRONIZE_PROPERTY_TOKEN}offsetWidth`]: false });
    Reactive.flush();

    element.style.width = '2px';
    element.style.height = '2px';
    element.dispatchEvent(new Event('event1'));

    expect(harness.synchronizedProperties.size).to.equal(1);
    const nodeMap = harness.synchronizedProperties.get(node)!;
    expect(nodeMap.size).to.equal(1);
    expect(nodeMap.has('offsetWidth')).to.equal(true);
  });

  it('does not flush pending debounced changes when a property is synchronized', async () => {
    // Ported from testFlushPendingChangesOnDomEvent.
    bind(node, element);

    let commandExecution = 0;
    const commands = new Map<string, () => void>([['prop', () => (commandExecution += 1)]]);

    let sendCommandExecution = 0;
    const debouncer = Debouncer.getOrCreate(element, 'on-value:false', 300);
    debouncer.trigger(new Set([EVENT_PHASE_TRAILING]), () => (sendCommandExecution += 1), commands);

    // The event synchronizes a property, so pending debounced changes are NOT
    // flushed.
    addListenerConstant('expressionsKey', { [`${SYNCHRONIZE_PROPERTY_TOKEN}offsetWidth`]: false });
    Reactive.flush();

    element.dispatchEvent(new Event('event1'));

    expect(sendCommandExecution, 'Changes should have not been flushed').to.equal(0);
    expect(commandExecution, 'Command should have not been run').to.equal(0);

    await waitForDebouncerToCleanUp();
  });

  it('flushes pending debounced changes when the event synchronizes nothing', async () => {
    // Ported from testDoNotFlushPendingChangesOnPropertySynchronization.
    bind(node, element);

    let commandExecution = 0;
    const commands = new Map<string, () => void>([['prop', () => (commandExecution += 1)]]);

    let sendCommandExecution = 0;
    const debouncer = Debouncer.getOrCreate(element, 'on-value:false', 300);
    debouncer.trigger(new Set([EVENT_PHASE_TRAILING]), () => (sendCommandExecution += 1), commands);

    // Empty expressions => no synchronized property => pending changes flushed.
    addListenerConstant('expressionsKey', {});
    Reactive.flush();

    element.dispatchEvent(new Event('event1'));

    expect(sendCommandExecution, 'Changes should have been flushed').to.equal(1);
    expect(commandExecution, 'Command should have been run').to.equal(1);

    await waitForDebouncerToCleanUp();
  });

  // Ported from GwtMultipleBindingTest.testDomEventHandlerDoubleBind: a second
  // bind must not re-read the element-listeners feature.
  it('binding twice does not re-read the element-listeners feature', () => {
    const guarded = new BindGuardStateNode(3, harness.tree, (m) => expect.fail(m));
    harness.tree.registerNode(guarded);
    guarded.getMap(NodeFeatures.ELEMENT_DATA);
    const guardedElement = document.createElement('div');

    bind(guarded, guardedElement);

    harness.constantPool.importFromJson({
      expressionsKey: { "window.navigator.userAgent[0] === 'M'": false }
    });
    guarded.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty('click').setValue('expressionsKey');
    Reactive.flush();

    guarded.setBound();
    bind(guarded, guardedElement);
    Reactive.flush();
  });
});

// Waits for cached Debouncers to be cleared by their idle timers so state does
// not leak between tests; mirrors GwtPropertyElementBinderTest.waitForDebouncerToCleanUp.
function waitForDebouncerToCleanUp(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 400));
}
