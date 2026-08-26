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

// Full-state-tree virtual-children fallback matrix ported from
// GwtBasicElementBinderTest. These bind a real host StateNode to a real element
// with a real (open) shadow root and assert the sendExistingElementWithIdAttach
// RPC arguments, mirroring the wrong-tag / no-corresponding-element /
// duplicate-attach / success cases for both @id and indices-path addressing.
//
// Two Java cases are intentionally not ported yet:
// testBindVirtualChild_withDeferredElementInShadowRoot_byId and
// ..._byIndicesPath. Their central assertion is that a deferred bind reverts a
// property value on flush, which is InitialPropertiesHandler behaviour; that
// class is not ported yet (the test harness stubs it as a no-op), so a faithful
// port has to wait for the InitialPropertiesHandler port rather than assert a
// faked revert.
describe('SimpleElementBindingStrategy virtual children', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let shadowRoot: ShadowRoot;
  let nextId: number;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    element = document.createElement('div');
    node.setDomNode(element);
    nextId = 10;
  });

  afterEach(() => Reactive.flush());

  // Mirrors addShadowRootElement: gives the element a real open shadow root and
  // marks it Polymer-ready (element.root + element.$).
  function addShadowRootElement(): ShadowRoot {
    const root = element.attachShadow({ mode: 'open' });
    (element as any).root = root;
    (element as any).$ = {};
    return root;
  }

  function createChildNode(id: string | null, tag: string): StateNode {
    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);
    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
    if (id !== null) {
      childNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').setValue(id);
    }
    return childNode;
  }

  // Mirrors addVirtualChild: sets ELEMENT_DATA/PAYLOAD to {type, payload} and adds
  // the child to the host's VIRTUAL_CHILDREN list.
  function addVirtualChild(childNode: StateNode, type: string, payload: unknown): void {
    const virtualChildren = node.getList(NodeFeatures.VIRTUAL_CHILDREN);
    childNode
      .getMap(NodeFeatures.ELEMENT_DATA)
      .getProperty(NodeProperties.PAYLOAD)
      .setValue({ [NodeProperties.TYPE]: type, [NodeProperties.PAYLOAD]: payload });
    virtualChildren.add(virtualChildren.length(), childNode);
  }

  // Mirrors createAndAppendElementToShadowRoot: creates a child in the shadow
  // root (recording it under element.$[id] when an id is given).
  function createAndAppendElementToShadowRoot(root: ShadowRoot, id: string | null, tagName: string): Element {
    const child = document.createElement(tagName);
    if (id !== null) {
      child.id = id;
      (element as any).$[id] = child;
    }
    root.appendChild(child);
    return child;
  }

  // Mirrors createAndAttachShadowRootNode: creates the shadow-root state node and
  // registers it as the host node's SHADOW_ROOT so its children are bound into the
  // element's shadow root.
  function createAndAttachShadowRootNode(): StateNode {
    const shadowRootNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(shadowRootNode);
    node.getMap(NodeFeatures.SHADOW_ROOT_DATA).getProperty(NodeProperties.SHADOW_ROOT).setValue(shadowRootNode);
    return shadowRootNode;
  }

  it('creates and binds an in-memory virtual child outside the element', () => {
    // Ported from testVirtualChild.
    bind(node, element);

    const childNode = createChildNode('child', 'span');
    addVirtualChild(childNode, NodeProperties.IN_MEMORY_CHILD, null);

    Reactive.flush();

    expect(element.childElementCount).to.equal(0);

    const childElement = childNode.getDomNode() as Element;
    expect(childElement.tagName).to.equal('SPAN');
    expect(childElement.id).to.equal('child');
  });

  it('reports a wrong-tag element found by id as a failed attach', () => {
    // Ported from testVirtualBindChild_wrongTag_searchById.
    shadowRoot = addShadowRootElement();
    const childId = 'childElement';
    const child = createChildNode(childId, 'a');
    addVirtualChild(child, NodeProperties.INJECT_BY_ID, childId);
    createAndAppendElementToShadowRoot(shadowRoot, 'otherId', 'div');

    bind(node, element);
    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, child.getId(), -1, childId]);
  });

  it('reports a missing element found by id as a failed attach', () => {
    // Ported from testVirtualBindChild_noCorrespondingElementInShadowRoot_searchById.
    shadowRoot = addShadowRootElement();
    const childId = 'childElement';
    const child = createChildNode(childId, 'a');
    addVirtualChild(child, NodeProperties.INJECT_BY_ID, childId);
    // An element with the right tag but a different id: still not found by id.
    createAndAppendElementToShadowRoot(shadowRoot, 'otherId', 'a');

    bind(node, element);
    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, child.getId(), -1, childId]);
  });

  it('reports a wrong-tag element found by indices path as a failed attach', () => {
    // Ported from testVirtualBindChild_wrongTag_searchByIndicesPath.
    shadowRoot = addShadowRootElement();
    const child = createChildNode(null, 'span');

    bind(node, element);

    addVirtualChild(child, NodeProperties.TEMPLATE_IN_TEMPLATE, [0]);
    createAndAppendElementToShadowRoot(shadowRoot, null, 'div');

    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, child.getId(), -1, null]);
  });

  it('reports a missing element found by indices path as a failed attach', () => {
    // Ported from testBindVirtualChild_noCorrespondingElementInShadowRoot_searchByIndicesPath.
    shadowRoot = addShadowRootElement();
    const child = createChildNode(null, 'span');

    bind(node, element);

    // The path points to index 1, but only index 0 exists.
    addVirtualChild(child, NodeProperties.TEMPLATE_IN_TEMPLATE, [1]);
    createAndAppendElementToShadowRoot(shadowRoot, null, 'span');

    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, child.getId(), -1, null]);
  });

  it('rejects a duplicate attach request addressed by id', () => {
    // Ported from testBindVirtualChild_doubleAttachRequest_searchById.
    shadowRoot = addShadowRootElement();
    const id = '@id';
    const childNode = createChildNode(id, element.tagName);
    const sameAttachDataChild = createChildNode(id, element.tagName);

    bind(node, element);

    addVirtualChild(childNode, NodeProperties.INJECT_BY_ID, id);
    createAndAppendElementToShadowRoot(shadowRoot, id, element.tagName);
    Reactive.flush();

    addVirtualChild(sameAttachDataChild, NodeProperties.INJECT_BY_ID, id);
    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, sameAttachDataChild.getId(), childNode.getId(), id]);
  });

  it('rejects a duplicate attach request addressed by indices path', () => {
    // Ported from testBindVirtualChild_doubleAttachRequest_searchByIndicesPath.
    shadowRoot = addShadowRootElement();
    const childNode = createChildNode(null, element.tagName);
    const sameAttachDataChild = createChildNode(null, element.tagName);

    bind(node, element);

    addVirtualChild(childNode, NodeProperties.TEMPLATE_IN_TEMPLATE, [0]);
    createAndAppendElementToShadowRoot(shadowRoot, null, element.tagName);
    Reactive.flush();

    addVirtualChild(sameAttachDataChild, NodeProperties.TEMPLATE_IN_TEMPLATE, [0]);
    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, sameAttachDataChild.getId(), childNode.getId(), null]);
  });

  it('reports a duplicate attach for an element already bound as a shadow-root child, addressed by id', () => {
    // Ported from testBindVirtualChild_existingShadowRootChildren_searchById: the
    // addressed element is already bound to a real shadow-root child node, so the
    // attach is reported as an already-attached duplicate (assignedId = that
    // child's node id).
    shadowRoot = addShadowRootElement();
    const id = '@id';
    const childNode = createChildNode(id, element.tagName);
    const virtualChild = createChildNode(id, element.tagName);

    const shadowRootNode = createAndAttachShadowRootNode();
    shadowRootNode.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);

    bind(node, element);
    Reactive.flush();

    addVirtualChild(virtualChild, NodeProperties.INJECT_BY_ID, id);
    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, virtualChild.getId(), childNode.getId(), id]);
  });

  it('reports a duplicate attach for an element already bound as a shadow-root child, addressed by indices path', () => {
    // Ported from testBindVirtualChild_existingShadowRootChildren_searchByIndicesPath.
    shadowRoot = addShadowRootElement();
    const childNode = createChildNode(null, element.tagName);
    const virtualChild = createChildNode(null, element.tagName);

    const shadowRootNode = createAndAttachShadowRootNode();
    shadowRootNode.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);

    bind(node, element);
    Reactive.flush();

    addVirtualChild(virtualChild, NodeProperties.TEMPLATE_IN_TEMPLATE, [0]);
    Reactive.flush();

    expect(harness.existingElementRpcArgs).to.deep.equal([node, virtualChild.getId(), childNode.getId(), null]);
  });

  it('attaches and binds a corresponding element found by id', () => {
    // Ported from testBindVirtualChild_withCorrespondingElementInShadowRoot_byId.
    shadowRoot = addShadowRootElement();
    const childId = 'childElement';
    const childNode = createChildNode(childId, element.tagName);
    childNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo').setValue('bar');

    bind(node, element);

    addVirtualChild(childNode, NodeProperties.INJECT_BY_ID, childId);
    const addressedElement = createAndAppendElementToShadowRoot(shadowRoot, childId, element.tagName);

    // Features that only appear after a successful bind.
    expect(childNode.hasFeature(NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS)).to.equal(false);
    expect(childNode.hasFeature(NodeFeatures.ELEMENT_CHILDREN)).to.equal(false);

    Reactive.flush();

    expect(childNode.hasFeature(NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS)).to.equal(true);
    expect(childNode.hasFeature(NodeFeatures.ELEMENT_CHILDREN)).to.equal(true);

    // No new light-DOM child, no extra shadow-root child, no failed-attach RPC.
    expect(element.childElementCount).to.equal(0);
    expect(shadowRoot.children.length).to.equal(1);
    expect(harness.existingElementRpcArgs).to.deep.equal([]);
    expect(childNode.getDomNode()).to.equal(addressedElement);
  });

  it('attaches and binds a corresponding element found by tag name and indices path', () => {
    // Ported from testBindVirtualChild_withCorrespondingElementInShadowRoot_byTagNameAndIndicesPath.
    shadowRoot = addShadowRootElement();
    const childNode = createChildNode(null, element.tagName);

    bind(node, element);

    addVirtualChild(childNode, NodeProperties.TEMPLATE_IN_TEMPLATE, [0]);
    const addressedElement = createAndAppendElementToShadowRoot(shadowRoot, null, element.tagName);

    expect(childNode.hasFeature(NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS)).to.equal(false);
    expect(childNode.hasFeature(NodeFeatures.ELEMENT_CHILDREN)).to.equal(false);

    Reactive.flush();

    expect(childNode.hasFeature(NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS)).to.equal(true);
    expect(childNode.hasFeature(NodeFeatures.ELEMENT_CHILDREN)).to.equal(true);

    expect(element.childElementCount).to.equal(0);
    expect(shadowRoot.children.length).to.equal(1);
    expect(harness.existingElementRpcArgs).to.deep.equal([]);
    expect(childNode.getDomNode()).to.equal(addressedElement);
  });
});
