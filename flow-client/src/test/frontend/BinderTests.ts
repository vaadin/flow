import { expect } from '@open-wc/testing';
import { bind } from '../../main/frontend/internal/binding/Binder';
import { ConstantPool } from '../../main/frontend/internal/ConstantPool';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { StateNode } from '../../main/frontend/internal/StateNode';

const ELEMENT_DATA = 0;
const TEXT_NODE = 7;

function makeTree(): any {
  const constantPool = new ConstantPool();
  const tree: any = {
    getNode: () => null,
    getRootNode: () => tree.rootNode,
    getFeatureDebugName: () => '',
    isActive: () => true,
    isVisible: () => true,
    isUpdateInProgress: () => false,
    sendNodePropertySyncToServer: () => {},
    sendEventToServer: () => {},
    sendExistingElementWithIdAttachToServer: () => {},
    getStateNodeForDomNode: () => null,
    getRegistry: () => ({
      getConstantPool: () => constantPool,
      getExistingElementMap: () => ({ getElement: () => null, remove: () => {} }),
      getInitialPropertiesHandler: () => ({ nodeRegistered: () => {}, flushPropertyUpdates: () => {} }),
      getApplicationConfiguration: () => ({ isWebComponentMode: () => false, getServiceUrl: () => '' })
    })
  };
  return tree;
}

describe('Binder', () => {
  afterEach(() => Reactive.flush());

  it('binds an element node via the simple-element strategy', () => {
    const tree = makeTree();
    const node = new StateNode(2, tree);
    node.getMap(ELEMENT_DATA).getProperty('tag').setValue('div');
    node.getMap(1).getProperty('title').setValue('hi'); // ELEMENT_PROPERTIES

    const element = document.createElement('div');
    node.setDomNode(element);
    bind(node, element);
    Reactive.flush();
    expect((element as any).title).to.equal('hi');
  });

  it('binds a text node via the text strategy', () => {
    const tree = makeTree();
    const node = new StateNode(3, tree);
    node.getMap(TEXT_NODE).getProperty('text').setValue('hello');

    const textNode = document.createTextNode('');
    node.setDomNode(textNode);
    bind(node, textNode);
    Reactive.flush();
    expect(textNode.data).to.equal('hello');
  });

  it('throws when no strategy is applicable', () => {
    const tree = makeTree();
    const node = new StateNode(4, tree); // no features, not the root
    expect(() => bind(node, document.createElement('div'))).to.throw('no suitable binder strategy');
  });
});
