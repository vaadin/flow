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

// Visibility binding is exercised the way GwtBasicElementBinderTest does it:
// bind a node to a real element, toggle the VISIBLE property and inspect the
// element's "hidden" attribute and inline display.
describe('SimpleElementBindingStrategy visibility binding', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let nextId: number;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    // The GWT fixture materialises the element-data feature up front, which is
    // what makes the node bindable by the simple element strategy.
    node.getMap(NodeFeatures.ELEMENT_DATA);
    element = document.createElement('div');
    nextId = 10;
  });

  afterEach(() => Reactive.flush());

  function setTag(): void {
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(element.tagName);
  }

  function setVisible(visible: boolean): void {
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE).setValue(visible);
  }

  function createChildNode(id: string, tag = 'span'): StateNode {
    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);
    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
    childNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').setValue(id);
    return childNode;
  }

  // Moves the element into a real shadow root, mirroring the shadow root the
  // GWT test fakes in addShadowRootElement.
  function addShadowRootElement(): void {
    const host = document.createElement('div');
    host.attachShadow({ mode: 'open' }).appendChild(element);
  }

  it('hides an invisible node without touching the display', () => {
    // Ported from testBindInvisibleNode.
    setVisible(false);

    bind(node, element);

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');
  });

  it('leaves a visible node unhidden', () => {
    // Ported from testBindVisibleNode.
    bind(node, element);

    expect(element.getAttribute('hidden')).to.equal(null);
  });

  it('binds the element only once the node becomes visible', () => {
    // Ported from testBindInvisibleElement_elementIsNotBound_elementBecomesBoundWhenVisible.
    setVisible(false);
    setTag();

    const childNode = createChildNode('child');
    node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);
    node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo').setValue('bar');

    node.setDomNode(element);

    let domNodeSetCount = 0;
    node.addDomNodeSetListener(() => {
      domNodeSetCount += 1;
      return false;
    });

    bind(node, element);

    Reactive.flush();

    expect(domNodeSetCount).to.equal(0);
    expect(element.childElementCount).to.equal(0);
    expect((element as unknown as Record<string, unknown>).foo).to.equal(undefined);

    setVisible(true);

    Reactive.flush();

    // The DOM node set listener is notified at least once.
    expect(domNodeSetCount).to.be.greaterThan(1);

    expect(element.childElementCount).to.equal(1);
    const tag = childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).getValue() as string;
    expect(element.firstElementChild!.tagName.toLowerCase()).to.equal(tag.toLowerCase());
    expect((element as unknown as Record<string, unknown>).foo).to.equal('bar');
  });

  it('removes the initial visibility listener when the node is unregistered', () => {
    // Ported from testBindInvisibleElement_unbind.
    setVisible(false);
    setTag();
    node.setDomNode(element);

    // Now the node is partially bound (it has a "visibility" listener).
    bind(node, element);

    Reactive.flush();

    // This rebinds the element, and has to remove the initial visibility listener.
    setVisible(true);

    Reactive.flush();

    harness.tree.unregisterNode(node);

    // Making the node invisible should now do nothing: all listeners after
    // REBOUND are removed, and the initial visibility listener of the partial
    // binding has to be removed as well.
    setVisible(false);

    Reactive.flush();

    // Had the listener been called, the attribute value would be "true".
    expect(element.getAttribute('hidden')).to.equal(null);
  });

  it('preserves the slot attribute of an invisible node', () => {
    // Ported from testBindInvisibleNode_slotAttributeIsPreserved.
    setVisible(false);
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('slot').setValue('drawer');

    bind(node, element);

    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.getAttribute('slot')).to.equal('drawer');
  });

  it('does not apply non-structural attributes of an invisible node', () => {
    // Ported from testBindInvisibleNode_nonStructuralAttributesAreNotApplied.
    setVisible(false);
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('data-info').setValue('secret');
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('slot').setValue('drawer');

    bind(node, element);

    Reactive.flush();

    expect(element.getAttribute('slot')).to.equal('drawer');
    expect(element.getAttribute('data-info')).to.equal(null);
  });

  it('keeps a hidden element hidden when the node is visible', () => {
    // Ported from testBindHiddenElement_stateNodeIsVisible_elementStaysHidden.
    element.setAttribute('hidden', 'true');

    bind(node, element);

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');
  });

  it('keeps a hidden element hidden across visibility changes', () => {
    // Ported from testBindHiddenElement_stateNodeChangesVisibility_elementStaysHidden.
    element.setAttribute('hidden', 'true');
    setTag();
    node.setDomNode(element);

    bind(node, element);

    setVisible(false);

    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');

    element.removeAttribute('hidden');

    setVisible(true);

    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');
  });

  it('hides an element in a shadow root with display none', () => {
    // Ported from testBindHiddenElement_elementInShadowRoot_elementHasDisplayNone.
    addShadowRootElement();
    setTag();
    node.setDomNode(element);

    bind(node, element);
    setVisible(false);
    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('none');
  });

  it('restores the initial display of a shadow-root element when unhidden', () => {
    // Ported from
    // testBindHiddenElement_elementInShadowRootAndHasInitialDisplayProperty_displayPropertyRestoredWhenUnhidden.
    addShadowRootElement();
    setTag();
    node.setDomNode(element);
    element.style.display = 'inline-block';

    bind(node, element);
    setVisible(false);
    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('none');

    setVisible(true);
    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal(null);
    expect(element.style.display).to.equal('inline-block');
  });

  it('unhides an initially not hidden element when the node becomes visible again', () => {
    // Ported from testBindNotHiddenElement_stateNodeChangesVisibility_elementIsNotHidden.
    setTag();
    node.setDomNode(element);

    bind(node, element);

    setVisible(false);

    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');

    setVisible(true);

    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal(null);
    expect(element.style.display).to.equal('');
  });

  it('keeps the initial not-hidden value even if the attribute is changed later', () => {
    // Ported from
    // testBindNotHiddenElement_stateNodeChangesVisibilityAndElementChangesHiddenValue_elementKeepsInitialNotHiddenValue.
    setTag();
    node.setDomNode(element);

    bind(node, element);

    // Make it invisible, make it visible, then change the "hidden" attribute.
    setVisible(false);
    Reactive.flush();
    setVisible(true);
    Reactive.flush();

    element.setAttribute('hidden', 'true');

    // Hide and unhide again.
    setVisible(false);
    Reactive.flush();
    setVisible(true);
    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal(null);
  });

  it('keeps a hidden element hidden when the node is initially invisible', () => {
    // Ported from testBindHiddenElement_stateNodeIsInvisible_elementStaysHidden.
    setVisible(false);
    element.setAttribute('hidden', 'true');
    setTag();
    node.setDomNode(element);

    // Now the node is partially bound (it has a "visibility" listener).
    bind(node, element);

    Reactive.flush();
    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');

    setVisible(true);

    Reactive.flush();

    expect(element.getAttribute('hidden')).to.equal('true');
    expect(element.style.display).to.equal('');
  });
});
