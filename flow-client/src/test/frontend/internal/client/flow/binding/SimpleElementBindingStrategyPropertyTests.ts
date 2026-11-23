import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { BindGuardStateNode, type CollectingTree, makeCollectingTree } from '../bindingTestHelpers';
import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { bind } from '../../../../../../main/frontend/internal/client/flow/binding/Binder';

// Element property binding is exercised the way GwtBasicElementBinderTest does
// it: bind a node to a real element, drive the ELEMENT_PROPERTIES map and read
// the JavaScript property off the element.
describe('SimpleElementBindingStrategy property binding', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let properties: ReturnType<StateNode['getMap']>;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);
    properties = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    element = document.createElement('div');
    node.setDomNode(element);
  });

  afterEach(() => Reactive.flush());

  it('binds a property that already has a value', () => {
    // Ported from testBindExistingProperty.
    properties.getProperty('title').setValue('foo');

    bind(node, element);

    Reactive.flush();

    expect(element.title).to.equal('foo');
  });

  it('keeps the DOM value type when the tree value only differs in type', () => {
    // Ported from testBindExistingPropertyWithDifferentType.
    // Set a number as the property value of the DOM element.
    const value = 42;
    (element as unknown as Record<string, unknown>).bar = value;

    // Set a string as the state tree property value.
    properties.getProperty('bar').setValue(String(value));

    bind(node, element);

    Reactive.flush();

    // The type should not be changed.
    expect(typeof (element as unknown as Record<string, unknown>).bar).to.equal('number');
  });

  it('binds a property whose value is set after binding', () => {
    // Ported from testBindNewProperty.
    bind(node, element);

    properties.getProperty('lang').setValue('foo');

    Reactive.flush();

    expect(element.lang).to.equal('foo');
  });

  it('applies an existing property eagerly at bind time, before any flush', () => {
    // Ported from testBindBeforeFlush. Binding eagerly applies property values
    // that already exist, so the value is visible without a Reactive.flush().
    properties.getProperty('title').setValue('foo');

    bind(node, element);

    expect(element.title).to.equal('foo');
  });

  it('does not apply a property set after bind until the next flush', () => {
    // Ported from testSetBeforeFlush. A value set after binding is only
    // scheduled; it stays unapplied until flush. Java asserts the GWT elemental
    // default "null"; a real DOM element reports the empty-string default here.
    bind(node, element);

    properties.getProperty('title').setValue('foo');

    expect(element.title).to.equal('');
  });

  it('applies nothing when the node is unregistered before the first flush', () => {
    // Ported from testUnbindBeforeFlush. Mirrors the GWT harness, whose root node
    // is created but never registered with the tree, so it can be unregistered.
    const unboundNode = new StateNode(0, harness.tree);
    const unboundElement = document.createElement('div');
    unboundNode.setDomNode(unboundElement);
    unboundNode.getMap(NodeFeatures.ELEMENT_DATA);
    const unboundProperties = unboundNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    const attributes = unboundNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);

    bind(unboundNode, unboundElement);

    unboundProperties.getProperty('title').setValue('foo');
    attributes.getProperty('id').setValue('foo');

    unboundNode.unregister();

    unboundProperties.getProperty('title').setValue('bar');
    attributes.getProperty('id').setValue('bar');
    attributes.getProperty('lang').setValue('newValue');

    Reactive.flush();

    // Java asserts the GWT elemental "null" default for the untouched title.
    expect(unboundElement.title).to.equal('');
    expect(unboundElement.id).to.equal('');
    expect(unboundElement.lang).to.equal('');
  });

  it('keeps the flushed values and ignores updates made after unregister', () => {
    // Ported from testUnbindAfterFlush. The root node is created but not
    // registered with the tree, matching the GWT harness, so it can unregister.
    const unboundNode = new StateNode(0, harness.tree);
    const unboundElement = document.createElement('div');
    unboundNode.setDomNode(unboundElement);
    unboundNode.getMap(NodeFeatures.ELEMENT_DATA);
    const unboundProperties = unboundNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    const attributes = unboundNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);

    bind(unboundNode, unboundElement);

    unboundProperties.getProperty('title').setValue('foo');
    attributes.getProperty('id').setValue('foo');

    Reactive.flush();

    unboundNode.unregister();

    unboundProperties.getProperty('title').setValue('bar');
    attributes.getProperty('id').setValue('bar');
    attributes.getProperty('lang').setValue('newValue');

    Reactive.flush();

    expect(unboundElement.title).to.equal('foo');
    expect(unboundElement.id).to.equal('foo');
    expect(unboundElement.lang).to.equal('');
  });

  it('deletes an arbitrary property when its value is removed', () => {
    // Ported from testRemoveArbitraryProperty.
    const foo = properties.getProperty('foo');
    foo.setValue('bar');

    bind(node, element);

    Reactive.flush();

    expect(Object.hasOwn(element, 'foo')).to.be.true;

    foo.removeValue();

    Reactive.flush();

    expect(Object.hasOwn(element, 'foo')).to.be.false;
  });

  it('clears a built-in property when its value is removed', () => {
    // Ported from testRemoveBuiltInProperty.
    const titleProperty = properties.getProperty('title');
    titleProperty.setValue('foo');

    bind(node, element);

    Reactive.flush();

    titleProperty.removeValue();

    Reactive.flush();

    // Properties inherited from e.g. Element can't be removed; assigning null
    // to title produces "null".
    expect(element.title).to.equal('null');
  });

  // Ported from GwtMultipleBindingTest.testSetPropertyDoubleBind: a second bind
  // must not re-read the element-properties feature.
  it('binding twice does not re-read the element-properties feature', () => {
    Reactive.reset();
    const { tree } = makeCollectingTree();
    const node = new BindGuardStateNode(2, tree, (m) => expect.fail(m));
    node.getMap(NodeFeatures.ELEMENT_DATA);
    const element = document.createElement('div');

    bind(node, element);
    node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo').setValue('bar');
    Reactive.flush();

    node.setBound();
    bind(node, element);
    Reactive.flush();
  });

  // Ported from GwtMultipleBindingTest.testSetAttributeDoubleBind: a second bind
  // must not re-read the element-attributes feature. The Java case in fact
  // guards ELEMENT_PROPERTIES, the same feature its sibling case guards, which
  // reads as a copy-paste slip; this guards the attributes feature its name
  // describes, which is strictly stronger.
  it('binding twice does not re-read the element-attributes feature', () => {
    Reactive.reset();
    const { tree } = makeCollectingTree();
    const node = new BindGuardStateNode(2, tree, (m) => expect.fail(m));
    node.getMap(NodeFeatures.ELEMENT_DATA);
    const element = document.createElement('div');

    bind(node, element);
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('foo').setValue('bar');
    Reactive.flush();

    node.setBound();
    bind(node, element);
    Reactive.flush();
  });
  // Beyond the Java suite: the guard against overwriting a DOM value the user
  // changed during a server round-trip has no GWT counterpart.
  describe('beyond the Java suite', () => {
    it('does not overwrite when the previous DOM value already matches the tree value', () => {
      const property = properties.getProperty('foo');
      property.setValue('bar');
      // The value the DOM had before the round-trip equals the tree value, so
      // the user-modified DOM value is kept.
      property.setPreviousDomValue('bar');
      (element as unknown as Record<string, unknown>).foo = 'user edit';

      bind(node, element);

      Reactive.flush();

      expect((element as unknown as Record<string, unknown>).foo).to.equal('user edit');
    });
  });
});
