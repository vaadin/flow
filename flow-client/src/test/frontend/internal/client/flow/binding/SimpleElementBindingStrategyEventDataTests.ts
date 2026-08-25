import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import {
  type CollectingTree,
  NodeFeatures,
  NodeProperties,
  StateNode,
  bind,
  makeCollectingTree
} from '../bindingTestHelpers';

// com.vaadin.flow.shared.JsonConstants.MAP_STATE_NODE_EVENT_DATA
const MAP_STATE_NODE_EVENT_DATA = ']';

// Event data collection is exercised through a bound element: the expressions
// of a DOM listener are evaluated when the event is dispatched, and the
// resulting data is what the tree sends to the server.
//
// Beyond the Java suite: the GWT suite covers plain data and filter expressions
// (ported in SimpleElementBindingStrategyEventListenerTests) but has no case for
// the debounce filters or the state-node mapping expressions below.
describe('SimpleElementBindingStrategy event data (beyond the Java suite)', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let nextId: number;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);
    element = document.createElement('div');
    node.setDomNode(element);
    document.body.appendChild(element);
    nextId = 10;
  });

  afterEach(() => {
    element.remove();
    Reactive.flush();
  });

  // Binds the node and registers a "click" listener with the given expressions.
  function bindWithClickExpressions(expressions: Record<string, unknown>): void {
    bind(node, element);
    harness.constantPool.importFromJson({ expressionsKey: expressions });
    node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty('click').setValue('expressionsKey');
    Reactive.flush();
  }

  // Adds a bound child element, so that the DOM subtree has more than one node
  // mapped to a state node.
  function addChild(tag: string): { childElement: Element; childNode: StateNode } {
    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);
    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
    node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);
    Reactive.flush();
    return { childElement: element.firstElementChild!, childNode };
  }

  it('sends the event immediately for a zero-timeout debounce filter', () => {
    // A debounce list of [[0]] is eager, so the filter matches at once.
    bindWithClickExpressions({ true: [[0]] });

    element.click();

    expect(harness.collectedNodes).to.have.length(1);
  });

  it('sends a leading-phase debounce immediately but buffers a trailing one', async () => {
    bindWithClickExpressions({ true: [[30, 'leading']] });

    element.click();
    expect(harness.collectedNodes).to.have.length(1);

    // Within the debounce period the event is swallowed.
    element.click();
    expect(harness.collectedNodes).to.have.length(1);

    // Once the debounce period is over, the next event is eager again.
    await new Promise((resolve) => {
      setTimeout(resolve, 50);
    });
    element.click();
    expect(harness.collectedNodes).to.have.length(2);
  });

  it('maps the event target to the closest state node id', () => {
    bindWithClickExpressions({ [MAP_STATE_NODE_EVENT_DATA]: false });
    const { childElement, childNode } = addChild('span');

    // The event bubbles up from the child, whose state node is the match.
    childElement.dispatchEvent(new Event('click', { bubbles: true }));

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData[MAP_STATE_NODE_EVENT_DATA]).to.equal(childNode.getId());
  });

  it('maps an event target without a state node to its closest bound ancestor', () => {
    bindWithClickExpressions({ [MAP_STATE_NODE_EVENT_DATA]: false });
    const { childElement, childNode } = addChild('span');
    const grandchild = document.createElement('b');
    childElement.appendChild(grandchild);

    grandchild.dispatchEvent(new Event('click', { bubbles: true }));

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    // The grandchild has no state node; the closest one is the child's.
    expect(eventData[MAP_STATE_NODE_EVENT_DATA]).to.equal(childNode.getId());
  });

  it('maps an element returned by an expression to the closest state node id', () => {
    const expression = `${MAP_STATE_NODE_EVENT_DATA}element.firstElementChild`;
    bindWithClickExpressions({ [expression]: false });
    const { childNode } = addChild('span');

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData[expression]).to.equal(childNode.getId());
  });

  it('maps an element outside the state tree to -1', () => {
    const expression = `${MAP_STATE_NODE_EVENT_DATA}document.head`;
    bindWithClickExpressions({ [expression]: false });

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData[expression]).to.equal(-1);
  });
});
