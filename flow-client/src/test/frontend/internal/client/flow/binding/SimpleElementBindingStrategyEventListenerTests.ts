import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { Debouncer } from '../../../../../../main/frontend/internal/client/flow/binding/Debouncer';
import { BindGuardStateNode, NodeFeatures, StateNode, bind, makeCollectingTree } from '../bindingTestHelpers';

// DOM event listener tests ported from GwtBasicElementBinderTest. They bind a
// real StateNode to a real element attached to the document and drive the
// listeners through the ELEMENT_LISTENERS map and the constant pool.
describe('SimpleElementBindingStrategy DOM event listeners', () => {
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

  // Adds a constant-pool entry for a "click" listener, mirroring the GWT
  // addToConstantPool + ELEMENT_LISTENERS setup.
  function addClickListenerConstant(key: string, expressions: Record<string, unknown>): void {
    harness.constantPool.importFromJson({ [key]: expressions });
    node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty('click').setValue(key);
  }

  it('sends the collected event and filter data to the server', () => {
    // Ported from testEventFired.
    bind(node, element);

    // The user agent is "Mozilla/5.0...".
    const booleanExpression = "window.navigator.userAgent[0] === 'M'";
    const numberExpression = 'event.button';
    const stringExpression = 'element.tagName';

    const trueFilter = 'true';
    const falseFilter = 'false';
    const tagNameFilter = "element.tagName == 'DIV'";

    addClickListenerConstant('expressionsKey', {
      // Data expressions.
      [booleanExpression]: false,
      [numberExpression]: false,
      [stringExpression]: false,
      // Filter expressions.
      [trueFilter]: true,
      [falseFilter]: true,
      [tagNameFilter]: true
    });
    Reactive.flush();

    element.click();

    expect(harness.collectedNodes).to.have.length(1);
    expect(harness.collectedNodes[0]).to.equal(node);

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;

    // 3 data expressions and 3 filter expressions.
    expect(Object.keys(eventData)).to.have.length(6);

    expect(typeof eventData[numberExpression]).to.equal('number');
    expect(eventData[stringExpression]).to.equal('DIV');
    expect(eventData[booleanExpression]).to.equal(true);

    expect(eventData[tagNameFilter]).to.equal(true);
    expect(eventData[trueFilter]).to.equal(true);
    expect(eventData[falseFilter]).to.equal(false);
  });

  it('does not send the event when a filter does not match', () => {
    // Ported from testFilterPreventsEvent.
    bind(node, element);

    addClickListenerConstant('expressionsKey', { false: true });
    Reactive.flush();

    element.click();

    expect(harness.collectedNodes).to.have.length(0);
  });

  it('sends the event when the falsy expression is not used as a filter', () => {
    // Ported from testEventFiredWithNoFilters.
    bind(node, element);

    // The expression is not used as a filter.
    addClickListenerConstant('expressionsKey', { false: false });
    Reactive.flush();

    element.click();

    expect(harness.collectedNodes).to.have.length(1);
  });

  it('does not send an event whose listener has been removed', () => {
    // Ported from testRemovedEventNotFired.
    bind(node, element);

    const clickEvent = node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty('click');
    clickEvent.setValue(1);

    Reactive.flush();

    clickEvent.removeValue();

    Reactive.flush();

    element.click();

    expect(harness.collectedNodes).to.have.length(0);
  });

  it('synchronizes only the event-specific property, not globally-marked ones', () => {
    // Ported from testDomListenerSynchronization.
    bind(node, element);

    // Only offsetWidth is requested by the event's expression, so only it is
    // synchronized. The Java test additionally marks offsetHeight through
    // setSyncProperties, but that writes a standalone NodeList the strategy
    // never reads, so the setup is inert there and is not ported.
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
