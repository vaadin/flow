import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import {
  type CollectingTree,
  type CollectingTreeOptions,
  NodeFeatures,
  NodeProperties,
  StateNode,
  bind,
  makeCollectingTree
} from '../bindingTestHelpers';

// The attribute binding is exercised the way GwtBasicElementBinderTest does it:
// bind a node to an element, set the attribute property, flush, and read the
// attribute back off the element.
function bindWithAttribute(
  tag: string,
  attribute: string,
  value: unknown,
  options: CollectingTreeOptions = {}
): { element: Element; harness: CollectingTree; node: StateNode } {
  const harness = makeCollectingTree(options);
  const node = new StateNode(2, harness.tree);
  harness.tree.registerNode(node);
  const element = document.createElement(tag);
  node.setDomNode(element);
  node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
  node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty(attribute).setValue(value);
  bind(node, element);
  Reactive.flush();
  return { element, harness, node };
}

describe('SimpleElementBindingStrategy attribute binding', () => {
  beforeEach(() => Reactive.reset());

  it('binds an attribute that already has a value', () => {
    // Ported from testBindExistingAttribute.
    const { element } = bindWithAttribute('div', 'id', 'foo');
    expect(element.getAttribute('id')).to.equal('foo');
  });

  it('binds an attribute whose value is set after binding', () => {
    // Ported from testBindNewAttribute.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    const element = document.createElement('div');
    node.setDomNode(element);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');

    bind(node, element);
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('lang').setValue('foo');
    Reactive.flush();

    expect(element.getAttribute('lang')).to.equal('foo');
  });

  it('applies an existing attribute eagerly at bind time, before any flush', () => {
    // Ported from testBindAttributeWithoutFlush.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    const element = document.createElement('div');
    node.setDomNode(element);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').setValue('foo');

    bind(node, element);

    expect(element.id).to.equal('foo');
  });

  it('does not apply an attribute set after bind until the next flush', () => {
    // Ported from testSetAttributeWithoutFlush.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    const element = document.createElement('div');
    node.setDomNode(element);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');

    bind(node, element);
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').setValue('foo');

    expect(element.id).to.equal('');
  });

  it('removes the attribute when its value is removed', () => {
    // Ported from testRemoveAttribute.
    const { element, node } = bindWithAttribute('div', 'id', 'foo');
    expect(element.getAttribute('id')).to.equal('foo');

    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').removeValue();
    Reactive.flush();

    expect(element.getAttribute('id')).to.equal(null);
  });

  // Beyond the Java suite: GwtBasicElementBinderTest only covers plain string
  // attributes, so the value coercion and the URI handling of the private
  // updateAttributeValue have no Java counterpart.
  describe('beyond the Java suite', () => {
    it('stringifies a non-string, non-object value', () => {
      const { element } = bindWithAttribute('div', 'tabindex', 3);
      expect(element.getAttribute('tabindex')).to.equal('3');
    });

    it('stringifies an array as its JSON text', () => {
      // Java stringifies a JsonArray as its JSON text, not as a comma-joined
      // list of its elements.
      const { element } = bindWithAttribute('div', 'data-list', ['a', 'b']);
      expect(element.getAttribute('data-list')).to.equal('["a","b"]');
    });

    it('applies a uri object as-is when not in web-component mode', () => {
      const { element } = bindWithAttribute('img', 'src', { uri: 'pic.png' });
      expect(element.getAttribute('src')).to.equal('pic.png');
    });

    it('prefixes the service url for a relative uri in web-component mode', () => {
      const { element } = bindWithAttribute(
        'img',
        'src',
        { uri: 'pic.png' },
        {
          webComponentMode: true,
          serviceUrl: 'http://host/app'
        }
      );
      expect(element.getAttribute('src')).to.equal('http://host/app/pic.png');
    });

    it('leaves an absolute uri untouched in web-component mode', () => {
      const { element } = bindWithAttribute(
        'img',
        'src',
        { uri: 'http://cdn/pic.png' },
        {
          webComponentMode: true,
          serviceUrl: 'http://host/app/'
        }
      );
      expect(element.getAttribute('src')).to.equal('http://cdn/pic.png');
    });
  });
});
