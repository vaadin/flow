import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import {
  BindGuardStateNode,
  type CollectingTree,
  NodeFeatures,
  NodeProperties,
  StateNode,
  bind,
  makeCollectingTree
} from '../bindingTestHelpers';

// Full-state-tree children tests ported from GwtBasicElementBinderTest. They
// bind a real StateNode to a real <div> via the real Binder and drive child
// insertion/removal through the ELEMENT_CHILDREN node list, mirroring the
// index/position edge cases the GWT suite covers.
describe('SimpleElementBindingStrategy children binding (full tree)', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let children: ReturnType<StateNode['getList']>;
  let nextId: number;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    element = document.createElement('div');
    node.setDomNode(element);
    children = node.getList(NodeFeatures.ELEMENT_CHILDREN);
    nextId = 10;
  });

  afterEach(() => Reactive.flush());

  function createChildNode(id: string | null, tag = 'span'): StateNode {
    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);
    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
    if (id !== null) {
      childNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').setValue(id);
    }
    return childNode;
  }

  // Appends a client-side-only element (no state node) to the parent, mirroring
  // GwtBasicElementBinderTest.createAndAppendElementToShadowRoot(element, ...).
  function appendClientSideChild(tag: string): Element {
    const clientChild = document.createElement(tag);
    element.appendChild(clientChild);
    return clientChild;
  }

  it('appends a DOM child for the added child node', () => {
    // Ported from testAddChild.
    bind(node, element);

    children.add(0, createChildNode('child'));

    Reactive.flush();

    expect(element.childElementCount).to.equal(1);
    const childElement = element.firstElementChild!;
    expect(childElement.tagName).to.equal('SPAN');
    expect(childElement.id).to.equal('child');
  });

  it('inserts children after the client-side children', () => {
    // Ported from testInsertChild.
    bind(node, element);

    appendClientSideChild('div');

    // With one client-side element, insert at 0 translates to index 1.
    children.add(0, createChildNode('first'));

    // <div/><span id="first"/>
    Reactive.flush();

    expect(element.childElementCount).to.equal(2);
    expect(element.children[0].tagName).to.equal('DIV');
    expect(element.children[1].tagName).to.equal('SPAN');
    expect(element.children[1].id).to.equal('first');

    // Insert at the first position, which is again translated to after the
    // client-side nodes.
    children.add(0, createChildNode('second', 'a'));

    // <div/><a id="second"/><span id="first"/>
    Reactive.flush();

    expect(element.childElementCount).to.equal(3);
    expect(element.children[0].tagName).to.equal('DIV');
    expect(element.children[1].tagName).to.equal('A');
    expect(element.children[1].id).to.equal('second');
    expect(element.children[2].id).to.equal('first');

    // A second client-side element, inserted between the existing children.
    const existingChild2 = document.createElement('div');
    element.insertBefore(existingChild2, element.children[1]);

    // Insert at the second position.
    children.add(1, createChildNode('third', 'h1'));

    // <div/><div/><a id="second"/><h1 id="third"/><span id="first"/>
    Reactive.flush();

    expect(element.childElementCount).to.equal(5);
    expect(element.children[1].tagName).to.equal('DIV');
    expect(element.children[3].tagName).to.equal('H1');
    expect(element.children[3].id).to.equal('third');

    // Insert after the last bound node.
    children.add(3, createChildNode('fourth', 'br'));

    // <div/><div/><a id="second"/><h1 id="third"/><span id="first"/><br id="fourth"/>
    Reactive.flush();

    expect(element.childElementCount).to.equal(6);
    // The element should be before the client side element and after the bound
    // node.
    expect(element.children[5].tagName).to.equal('BR');
    expect(element.children[5].id).to.equal('fourth');
  });

  it('removes the DOM child of a spliced-out child node', () => {
    // Ported from testRemoveChild.
    bind(node, element);

    children.add(0, createChildNode(null));

    Reactive.flush();

    expect(element.childElementCount).to.equal(1);
    const childElement = element.firstElementChild!;

    children.splice(0, 1);

    Reactive.flush();

    expect(element.childElementCount).to.equal(0);
    expect(childElement.parentElement).to.equal(null);
  });

  it('binds only the children left after a clear', () => {
    // Ported from testClearChildren.
    element.appendChild(document.createElement('a'));
    element.appendChild(document.createElement('hr'));

    children.add(0, createChildNode('foo'));
    children.clear();
    children.add(0, createChildNode('bar'));

    bind(node, element);

    expect(element.childElementCount).to.equal(1);
    const childElement = element.firstElementChild!;
    expect(childElement.tagName.toLowerCase()).to.equal('span');

    Reactive.flush();
    expect(childElement.getAttribute('id')).to.equal('bar');

    children.splice(0, 1);

    Reactive.flush();

    expect(element.childElementCount).to.equal(0);
    expect(childElement.parentElement).to.equal(null);
  });

  it('inserts a bound child after pre-existing client-side children', () => {
    // Ported from testInsertChildAfterExistingChildren.
    const existingChild1 = document.createElement('span');
    const existingChild2 = document.createElement('span');
    element.appendChild(existingChild1);
    element.appendChild(existingChild2);

    bind(node, element);
    Reactive.flush();
    expect(element.childElementCount).to.equal(2);

    children.add(0, createChildNode('first', 'div'));
    Reactive.flush();

    expect(element.childElementCount).to.equal(3);
    expect(element.children[0].tagName).to.equal('SPAN');
    expect(element.children[1].tagName).to.equal('SPAN');
    expect(element.children[2].tagName).to.equal('DIV');
  });

  it('recalculates the insertion index across not-yet-bound nodes and a client-side child', () => {
    // Ported from testInsertChild_recalculateIndex.
    bind(node, element);

    appendClientSideChild('div');

    // The order is important: some state nodes don't yet have a DOM node when the
    // splices are processed, so the insertion index must be recomputed.
    children.add(0, createChildNode('first'));
    children.add(1, createChildNode('second'));
    children.add(0, createChildNode('third'));

    Reactive.flush();

    expect(element.childElementCount).to.equal(4);
    expect(element.children[0].tagName).to.equal('DIV');
    expect(element.children[1].id).to.equal('third');
    expect(element.children[2].id).to.equal('first');
    expect(element.children[3].id).to.equal('second');
  });

  it('re-adds a removed node reusing its existing DOM node', () => {
    // Ported from testReAddNode.
    bind(node, element);

    const childToReadd = createChildNode('2');
    children.splice(0, 0, [createChildNode('1'), childToReadd, createChildNode('3')]);

    Reactive.flush();

    const readdedDom = childToReadd.getDomNode();
    expect(element.childElementCount).to.equal(3);

    children.splice(1, 1);
    children.splice(1, 0, [childToReadd]);

    Reactive.flush();

    const statNode = children.get(1) as StateNode;
    expect(statNode).to.equal(childToReadd);
    expect(statNode.getDomNode()).to.equal(readdedDom);
  });

  it('adds the replacement before removing the old child on clear-and-replace', () => {
    // Ported from testClearAndAddChildren_replacementAddedBeforeOldChildRemoved.
    bind(node, element);

    children.add(0, createChildNode('old'));
    Reactive.flush();

    const oldElement = element.firstElementChild!;

    children.clear();
    children.add(0, createChildNode('new'));

    // A flush listener registered after the splices runs once the children
    // handlers are done but before the old children are removed, which is where
    // the container must not have been emptied: an empty container loses the
    // scroll position of the scrollable element around it.
    let childCountWhileReplacing = 0;
    Reactive.addFlushListener(() => {
      childCountWhileReplacing = element.childElementCount;
    });

    Reactive.flush();

    expect(childCountWhileReplacing).to.equal(2);

    expect(element.childElementCount).to.equal(1);
    expect(element.firstElementChild!.id).to.equal('new');
    expect(oldElement.parentElement).to.equal(null);
  });

  it('empties the container immediately on a clear with no replacement', () => {
    // Ported from testClearChildrenWithoutReplacement_allNodesRemoved.
    bind(node, element);

    children.add(0, createChildNode('child'));
    Reactive.flush();

    // a node the server does not know about
    appendClientSideChild('a');

    children.clear();
    Reactive.flush();

    expect(element.childElementCount).to.equal(0);
  });

  it('keeps re-added and moved children when the container is cleared', () => {
    // Ported from testClearChildren_reAddedAndMovedChildrenKept.
    bind(node, element);

    const container = createChildNode(null, 'div');
    const childNode = createChildNode('child');
    children.add(0, container);
    children.add(1, childNode);
    Reactive.flush();

    const containerElement = element.children[0];
    const childElement = element.children[1];

    // the server empties the node, adds the container back and moves the child
    // into the container
    children.clear();
    children.add(0, container);
    container.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);

    Reactive.flush();

    expect(element.childElementCount).to.equal(1);
    expect(element.firstElementChild).to.equal(containerElement);
    expect(containerElement.childElementCount).to.equal(1);
    expect(containerElement.firstElementChild).to.equal(childElement);
  });

  it('removes a child at the right position despite an unofficial extra child', () => {
    // Ported from testRemoveChildPosition.
    bind(node, element);

    const childNode = createChildNode('child');
    children.add(0, childNode);
    Reactive.flush();

    const firstChildElement = element.firstElementChild!;

    // Add an "unofficial" (client-side) child to mess with index computations.
    const extraChild = document.createElement('img');
    element.insertBefore(extraChild, firstChildElement);

    children.splice(0, 1);
    Reactive.flush();

    expect(element.childNodes.length).to.equal(1);
    expect(element.childNodes[0]).to.equal(extraChild);
    expect(firstChildElement.parentElement).to.equal(null);
  });

  it('adds several children and removes an interior range in one splice', () => {
    // Ported from testAddRemoveMultiple.
    bind(node, element);

    children.splice(0, 0, [createChildNode('1'), createChildNode('2'), createChildNode('3'), createChildNode('4')]);

    Reactive.flush();

    expect(element.childElementCount).to.equal(4);

    const child1 = element.children[0];
    const child2 = element.children[1];
    const child3 = element.children[2];
    const child4 = element.children[3];

    expect(child1.id).to.equal('1');
    expect(child2.id).to.equal('2');
    expect(child3.id).to.equal('3');
    expect(child4.id).to.equal('4');

    children.splice(1, 2);

    Reactive.flush();

    expect(element.childElementCount).to.equal(2);
    expect(element.childNodes[0]).to.equal(child1);
    expect(element.childNodes[1]).to.equal(child4);
  });

  it('adopts an element registered as an existing element instead of creating one', () => {
    // Ported from testAttachExistingElement.
    bind(node, element);

    const childNode = createChildNode('child');
    const tag = childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).getValue() as string;

    // Create and add an existing element.
    const span = document.createElement(tag);
    element.appendChild(span);
    harness.existingElementMap.add(childNode.getId(), span);

    children.add(0, childNode);

    Reactive.flush();

    // Nothing has changed: no new child.
    expect(element.childElementCount, 'No new child should appear in the element').to.equal(1);
    const childElement = element.firstElementChild!;
    expect(childElement.tagName.toLowerCase()).to.equal(tag);
    expect(childElement).to.equal(span);
    expect(childElement.id).to.equal('child');
    expect(harness.existingElementMap.getElement(childNode.getId())).to.equal(null);
  });

  it('binds a child that is added before its tag is set', () => {
    // Ported from testAddBeforeSetTag.
    bind(node, element);

    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);

    children.add(0, childNode);

    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('span');

    // Should not throw.
    Reactive.flush();

    expect(element.childElementCount).to.equal(1);
  });

  it('adds a text-node child and applies later text only on the next flush', () => {
    // Ported from testAddTextNode.
    bind(node, element);

    const textNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(textNode);
    const textProperty = textNode.getMap(NodeFeatures.TEXT_NODE).getProperty(NodeProperties.TEXT);
    textProperty.setValue('foo');

    children.add(0, textNode);
    Reactive.flush();

    expect(element.textContent).to.equal('foo');

    // A later text change stays unapplied until the next flush.
    textProperty.setValue('bar');
    expect(element.textContent).to.equal('foo');

    Reactive.flush();
    expect(element.textContent).to.equal('bar');
  });

  it('removes a text-node child that is spliced out', () => {
    // Ported from testRemoveTextNode.
    bind(node, element);

    const textNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(textNode);
    textNode.getMap(NodeFeatures.TEXT_NODE).getProperty(NodeProperties.TEXT).setValue('foo');

    children.add(0, textNode);
    Reactive.flush();

    expect(element.childNodes.length).to.equal(1);

    children.splice(0, 1);
    Reactive.flush();

    expect(element.childNodes.length).to.equal(0);
  });

  // Ported from GwtMultipleBindingTest.testAddChildDoubleBind: a second bind must
  // not re-read the element-children feature.
  it('binding twice does not re-read the element-children feature', () => {
    const guarded = new BindGuardStateNode(3, harness.tree, (m) => expect.fail(m));
    harness.tree.registerNode(guarded);
    guarded.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    const guardedElement = document.createElement('div');
    guarded.setDomNode(guardedElement);

    bind(guarded, guardedElement);

    const childNode = createChildNode('child');
    guarded.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);
    Reactive.flush();

    guarded.setBound();
    bind(guarded, guardedElement);
    Reactive.flush();
  });
});
