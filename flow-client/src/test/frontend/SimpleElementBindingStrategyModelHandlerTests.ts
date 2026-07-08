import { expect } from '@open-wc/testing';
import { UpdatableModelProperties } from '../../main/frontend/internal/model/UpdatableModelProperties';
import {
  handleListItemPropertyChange,
  handlePropertiesChanged,
  handlePropertyChange,
  InitialPropertyUpdate
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import {
  BindGuardStateNode,
  type CollectingTree,
  NodeFeatures,
  NodeProperties,
  StateNode,
  bind,
  makeCollectingTree
} from './bindingTestHelpers';

const ELEMENT_PROPERTIES = 1;

// A StateNode stand-in for the model handlers: node data keyed by class plus a
// single ELEMENT_PROPERTIES map of scalar properties.
function fakeModelNode(config: {
  updatable?: UpdatableModelProperties | null;
  initialUpdate?: InitialPropertyUpdate | null;
  properties?: Record<string, { getValue(): unknown; syncToServer(value: unknown): void }>;
}): any {
  const properties = config.properties ?? {};
  const map = {
    hasPropertyValue: (name: string) => name in properties,
    getProperty: (name: string) => properties[name]
  };
  return {
    getNodeData: (clazz: unknown) => {
      if (clazz === UpdatableModelProperties) {
        return config.updatable ?? null;
      }
      if (clazz === InitialPropertyUpdate) {
        return config.initialUpdate ?? null;
      }
      return null;
    },
    getMap: () => map
  };
}

describe('SimpleElementBindingStrategy model handlers', () => {
  it('InitialPropertyUpdate runs the command once and clears itself', () => {
    const cleared: unknown[] = [];
    const ran: string[] = [];
    const update = new InitialPropertyUpdate({ clearNodeData: (o: object) => cleared.push(o) });
    update.setCommand(() => ran.push('run'));
    update.execute();
    expect(ran).to.deep.equal(['run']);
    expect(cleared).to.deep.equal([update]);
  });

  it('handleListItemPropertyChange syncs only when the node has element properties', () => {
    const synced: unknown[] = [];
    const node: any = {
      hasFeature: (feature: number) => feature === ELEMENT_PROPERTIES,
      getMap: () => ({ getProperty: () => ({ syncToServer: (v: unknown) => synced.push(v) }) })
    };
    handleListItemPropertyChange(5, null, 'value', 'x', { getNode: (id: number) => (id === 5 ? node : null) });
    expect(synced).to.deep.equal(['x']);

    // No ELEMENT_PROPERTIES feature => no sync.
    handleListItemPropertyChange(6, null, 'value', 'y', { getNode: () => ({ hasFeature: () => false }) } as any);
    expect(synced).to.deep.equal(['x']);
  });

  it('handlePropertyChange syncs an updatable scalar property', () => {
    const synced: unknown[] = [];
    const node = fakeModelNode({
      updatable: new UpdatableModelProperties(['name']),
      properties: { name: { getValue: () => 'old', syncToServer: (v) => synced.push(v) } }
    });
    handlePropertyChange('name', () => 'Bob', node);
    expect(synced).to.deep.equal(['Bob']);
  });

  it('handlePropertyChange ignores non-updatable properties and missing data', () => {
    const synced: unknown[] = [];
    const property = { getValue: () => 'old', syncToServer: (v: unknown) => synced.push(v) };

    handlePropertyChange(
      'name',
      () => 'Bob',
      fakeModelNode({ updatable: new UpdatableModelProperties([]), properties: { name: property } })
    );
    handlePropertyChange('name', () => 'Bob', fakeModelNode({ updatable: null, properties: { name: property } }));
    expect(synced).to.deep.equal([]);
  });

  it('handlePropertiesChanged runs immediately, or defers to the initial update', () => {
    const synced: unknown[] = [];
    const properties = { name: { getValue: () => 'old', syncToServer: (v: unknown) => synced.push(v) } };

    // No pending initial update => runs now.
    handlePropertiesChanged(
      { name: 'Bob' },
      fakeModelNode({ updatable: new UpdatableModelProperties(['name']), properties })
    );
    expect(synced).to.deep.equal(['Bob']);

    // Pending initial update => deferred until execute().
    synced.length = 0;
    const initialUpdate = new InitialPropertyUpdate({ clearNodeData: () => {} });
    handlePropertiesChanged(
      { name: 'Jane' },
      fakeModelNode({ updatable: new UpdatableModelProperties(['name']), initialUpdate, properties })
    );
    expect(synced).to.deep.equal([]);
    initialUpdate.execute();
    expect(synced).to.deep.equal(['Jane']);
  });
});

// Full-state-tree Polymer model tests ported from GwtPolymerModelTest. They bind
// a real StateNode to a Polymer-model element (mocked set/splice) via the real
// Binder, and drive model-list changes through the TEMPLATE_MODELLIST feature.
describe('SimpleElementBindingStrategy Polymer model (full tree)', () => {
  const MODEL_PROPERTY_NAME = 'model';
  const LIST_PROPERTY_NAME = 'listProperty';
  const TEMPLATE_MODELLIST = NodeFeatures.TEMPLATE_MODELLIST;

  let harness: CollectingTree;
  let node: StateNode;
  let modelNode: StateNode;
  let element: any;
  let nextId: number;

  // A Polymer element mock mirroring GwtPolymerModelTest.createHtmlElement +
  // initPolymer: a P3 Polymer element (recognized via
  // constructor.polymerElementVersion) whose set() writes a dotted path and
  // whose splice() records its arguments in argumentsArray.
  function createPolymerElement(): any {
    const el: any = document.createElement('custom-div');
    Object.defineProperty(el, 'constructor', { value: { polymerElementVersion: '2.0.1' }, configurable: true });
    el.set = (path: string, newValue: unknown): void => {
      const split = path.split('.');
      let prop: any = el;
      for (let i = 0; i < split.length - 1; i++) {
        if (!prop) {
          break;
        }
        prop = prop[split[i]];
      }
      if (prop) {
        prop[split[split.length - 1]] = newValue;
      }
    };
    el.splice = (...args: unknown[]): void => {
      if (el.argumentsArray) {
        el.argumentsArray.push(args);
      } else {
        el.argumentsArray = [args];
      }
    };
    return el;
  }

  function setModelProperty(stateNode: StateNode, name: string, value: unknown, flush: boolean): void {
    stateNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(name).setValue(value);
    if (value instanceof StateNode) {
      value.setParent(stateNode);
    }
    if (flush) {
      Reactive.flush();
    }
  }

  function createAndAttachModelNode(modelPropertyName: string): StateNode {
    const mNode = new StateNode(nextId, harness.tree);
    harness.tree.registerNode(mNode);
    mNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    nextId++;
    setModelProperty(node, modelPropertyName, mNode, true);
    return mNode;
  }

  function fillNodeWithListItems(listNode: StateNode, listItems: string[]): void {
    const nodeList = listNode.getList(TEMPLATE_MODELLIST);
    for (let i = 0; i < listItems.length; i++) {
      nodeList.add(i, listItems[i]);
    }
  }

  function createAndAttachNodeWithList(mNode: StateNode, listItems: string[]): StateNode {
    const nodeWithList = new StateNode(nextId, harness.tree);
    harness.tree.registerNode(nodeWithList);
    nextId++;
    fillNodeWithListItems(nodeWithList, listItems);
    setModelProperty(mNode, LIST_PROPERTY_NAME, nodeWithList, false);
    return nodeWithList;
  }

  function getClientList(): string[] {
    return element[MODEL_PROPERTY_NAME][LIST_PROPERTY_NAME];
  }

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
    node.getMap(NodeFeatures.ELEMENT_DATA);
    nextId = node.getId() + 1;
    modelNode = createAndAttachModelNode(MODEL_PROPERTY_NAME);
    element = createPolymerElement();
    node.setDomNode(element);
  });

  afterEach(() => Reactive.flush());

  it('adds a model list to the element', () => {
    // Ported from testAddList.
    const serverList = ['one', 'two'];
    createAndAttachNodeWithList(modelNode, serverList);

    bind(node, element);
    Reactive.flush();

    expect(getClientList()).to.deep.equal(serverList);
  });

  it('replaces the model list when a new list node is set for the same property', () => {
    // Ported from testSetNewListForTheSameProperty.
    createAndAttachNodeWithList(modelNode, ['one', 'two']);

    bind(node, element);
    Reactive.flush();

    const newServerList = ['1', '2', '3'];
    createAndAttachNodeWithList(modelNode, newServerList);
    Reactive.flush();

    expect(getClientList()).to.deep.equal(newServerList);
  });

  it('pushes each list addition to the element via splice', () => {
    // Ported from testUpdateList / assertUpdateListValues.
    const nodeWithList = createAndAttachNodeWithList(modelNode, ['one', 'two']);

    bind(node, element);
    Reactive.flush();

    const newList = ['1', '2', '3'];
    fillNodeWithListItems(nodeWithList, newList);
    Reactive.flush();

    // Each add is a separate splice call, so there are as many splice calls as
    // added items, each with [path, start, deleteCount, item].
    const argumentsArray: unknown[][] = element.argumentsArray;
    expect(argumentsArray.length).to.equal(newList.length);
    for (let i = 0; i < newList.length; i++) {
      const args = argumentsArray[i];
      expect(args.length).to.equal(4);
      expect(args[0]).to.equal(`${MODEL_PROPERTY_NAME}.${LIST_PROPERTY_NAME}`);
      expect(args[1]).to.equal(i);
      expect(args[2]).to.equal(0);
      expect(args[3]).to.equal(newList[i]);
    }
  });

  it('ignores list updates after the nodes are unregistered', () => {
    // Ported from testListUpdatesAreIgnoredAfterUnregister.
    const serverList = ['one', 'two'];
    const nodeWithList = createAndAttachNodeWithList(modelNode, serverList);

    bind(node, element);
    Reactive.flush();

    harness.tree.unregisterNode(node);
    harness.tree.unregisterNode(modelNode);

    fillNodeWithListItems(nodeWithList, ['1', '2', '3']);
    Reactive.flush();

    expect(getClientList()).to.deep.equal(serverList);
  });

  // Ported from GwtMultipleBindingTest.testBindModelPropertiesDoubleBind: a
  // second bind of a Polymer element must not re-read the element-properties
  // feature.
  it('binding twice does not re-read model properties', () => {
    const guarded = new BindGuardStateNode(50, harness.tree, (m) => expect.fail(m));
    harness.tree.registerNode(guarded);
    guarded.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('custom-div');
    const guardedElement = createPolymerElement();
    guarded.setDomNode(guardedElement);

    bind(guarded, guardedElement);
    guarded.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo').setValue('bar');
    Reactive.flush();

    guarded.setBound();
    bind(guarded, guardedElement);
    Reactive.flush();
  });
});
