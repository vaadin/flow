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

// Style and class-list binding is exercised the way GwtBasicElementBinderTest
// does it: bind a node to a real element and inspect the element afterwards.
function makeBoundNode(): { element: HTMLElement; harness: CollectingTree; node: StateNode } {
  const harness = makeCollectingTree();
  const node = new StateNode(2, harness.tree);
  harness.tree.registerNode(node);
  const element = document.createElement('div');
  node.setDomNode(element);
  node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
  return { element, harness, node };
}

// The three style values asserted by testAddStylesBeforeBind/AfterBind: a plain
// value, one with an !important priority, and one whose value merely starts
// with "!important" and therefore carries no priority.
function setStyles(node: StateNode): void {
  const styles = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
  styles.getProperty('color').setValue('green');
  styles.getProperty('display').setValue('none !important');
  // The GWT test uses "!importantfoo", relying on a polyfilled setProperty to
  // make an invalid value stick; a real browser drops any value with a
  // top-level "!". A quoted string keeps the same case - a value that contains
  // "!important" without it being a priority is applied verbatim.
  styles.getProperty('content').setValue('"!important"');
}

function expectStyles(element: HTMLElement): void {
  expect(element.style.getPropertyValue('color')).to.equal('green');
  expect(element.style.getPropertyValue('display')).to.equal('none');
  expect(element.style.getPropertyPriority('display')).to.equal('important');
  expect(element.style.getPropertyValue('content')).to.equal('"!important"');
  expect(element.style.getPropertyPriority('content')).to.equal('');
}

describe('SimpleElementBindingStrategy styling binding', () => {
  beforeEach(() => Reactive.reset());

  it('applies styles that were set before binding', () => {
    // Ported from testAddStylesBeforeBind.
    const { element, node } = makeBoundNode();
    setStyles(node);

    bind(node, element);
    Reactive.flush();

    expectStyles(element);
  });

  it('applies styles that are set after binding', () => {
    // Ported from testAddStylesAfterBind.
    const { element, node } = makeBoundNode();
    bind(node, element);
    setStyles(node);

    Reactive.flush();

    expectStyles(element);
  });

  it('removes a style property when its value is removed', () => {
    // Ported from testRemoveStyles.
    const { element, node } = makeBoundNode();
    bind(node, element);

    const styles = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
    styles.getProperty('background').setValue('blue');
    styles.getProperty('color').setValue('white');

    Reactive.flush();
    expect(element.style.getPropertyValue('background')).to.equal('blue');
    expect(element.style.getPropertyValue('color')).to.equal('white');

    styles.getProperty('color').removeValue();

    Reactive.flush();
    expect(element.style.getPropertyValue('background')).to.equal('blue');
    expect(element.style.getPropertyValue('color')).to.equal('');
  });

  it('stops applying styles once the node is unregistered', () => {
    // Ported from testAddStylesAfterUnbind.
    const { element, harness, node } = makeBoundNode();
    bind(node, element);

    const styles = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
    styles.getProperty('color').setValue('red');
    Reactive.flush();

    harness.tree.unregisterNode(node);

    styles.getProperty('color').setValue('blue');
    styles.getProperty('font-size').setValue('12px');

    Reactive.flush();
    expect(element.style.getPropertyValue('color')).to.equal('red');
  });

  it('applies classes that were added before binding', () => {
    // Ported from testAddClassesBeforeBind.
    const { element, node } = makeBoundNode();
    node.getList(NodeFeatures.CLASS_LIST).add(0, 'foo');

    bind(node, element);

    expect(element.className).to.equal('foo');
  });

  it('applies classes that are added after binding', () => {
    // Ported from testAddClassesAfterBind.
    const { element, node } = makeBoundNode();
    bind(node, element);

    node.getList(NodeFeatures.CLASS_LIST).add(0, 'foo');

    expect(element.className).to.equal('foo');
  });

  it('removes spliced classes', () => {
    // Ported from testRemoveClasses.
    const { element, node } = makeBoundNode();
    bind(node, element);

    node.getList(NodeFeatures.CLASS_LIST).splice(0, 0, ['one', 'two', 'three']);
    expect(element.className).to.equal('one two three');

    node.getList(NodeFeatures.CLASS_LIST).splice(1, 1);
    expect(element.className).to.equal('one three');
  });

  it('stops applying classes once the node is unregistered', () => {
    // Ported from testAddClassesAfterUnbind.
    const { element, harness, node } = makeBoundNode();
    bind(node, element);

    node.getList(NodeFeatures.CLASS_LIST).add(0, 'foo');

    harness.tree.unregisterNode(node);

    node.getList(NodeFeatures.CLASS_LIST).add(0, 'bar');

    expect(element.className).to.equal('foo');
  });

  // Ported from GwtMultipleBindingTest.testAddClassListDoubleBind: a second bind
  // must not re-read the class-list feature.
  it('binding twice does not re-read the class-list feature', () => {
    const { tree } = makeCollectingTree();
    const node = new BindGuardStateNode(2, tree, (m) => expect.fail(m));
    node.getMap(NodeFeatures.ELEMENT_DATA);
    node.getList(NodeFeatures.CLASS_LIST).add(0, 'foo');
    const guardedElement = document.createElement('div');

    bind(node, guardedElement);
    Reactive.flush();

    node.setBound();
    bind(node, guardedElement);
    Reactive.flush();
  });

  // Ported from GwtMultipleBindingTest.testAddStylesDoubleBind: binding the same
  // node a second time must be a no-op that never re-reads the style feature.
  it('binding twice does not re-read the style-properties feature', () => {
    const { tree } = makeCollectingTree();
    const node = new BindGuardStateNode(2, tree, (m) => expect.fail(m));
    node.getMap(NodeFeatures.ELEMENT_DATA);
    const element = document.createElement('div');

    bind(node, element);
    node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty('foo').setValue('bar');
    Reactive.flush();

    node.setBound();
    // A second bind must return immediately without touching any feature.
    bind(node, element);
    Reactive.flush();
  });
});
