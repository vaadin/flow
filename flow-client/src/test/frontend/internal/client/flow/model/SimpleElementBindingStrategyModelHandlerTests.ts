import { expect } from '@open-wc/testing';
import { UpdatableModelProperties } from '../../../../../../main/frontend/internal/client/flow/model/UpdatableModelProperties';
import { setProperty } from '../../../../../../main/frontend/internal/client/PolymerUtils';
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

// Full-state-tree Polymer model tests ported from GwtPolymerModelTest. They bind
// a real StateNode to a Polymer-model element (mocked set/splice) via the real
// Binder, and drive model-list changes through the TEMPLATE_MODELLIST feature.
describe('SimpleElementBindingStrategy Polymer model', () => {
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

  // Mirrors GwtPolymerModelTest's Function<Object, ?> converter: identity leaves
  // the list item as a raw value, createBasicTypeWrapper wraps it in a
  // BASIC_TYPE_VALUE node.
  type ListItemConverter = (value: string) => unknown;

  const identityConverter: ListItemConverter = (value) => value;

  // Wraps a value in a state node carrying only the BASIC_TYPE_VALUE feature, the
  // way the server represents a list of primitives. Mirrors createBasicTypeWrapper.
  function createBasicTypeWrapper(value: string): StateNode {
    const wrapper = new StateNode(nextId, harness.tree);
    harness.tree.registerNode(wrapper);
    nextId++;
    wrapper.getMap(NodeFeatures.BASIC_TYPE_VALUE).getProperty(NodeProperties.VALUE).setValue(value);
    return wrapper;
  }

  function fillNodeWithListItems(
    listNode: StateNode,
    listItems: string[],
    converter: ListItemConverter = identityConverter
  ): void {
    const nodeList = listNode.getList(TEMPLATE_MODELLIST);
    for (let i = 0; i < listItems.length; i++) {
      nodeList.add(i, converter(listItems[i]));
    }
  }

  function createAndAttachNodeWithList(
    mNode: StateNode,
    listItems: string[],
    converter: ListItemConverter = identityConverter
  ): StateNode {
    const nodeWithList = new StateNode(nextId, harness.tree);
    harness.tree.registerNode(nodeWithList);
    nextId++;
    fillNodeWithListItems(nodeWithList, listItems, converter);
    setModelProperty(mNode, LIST_PROPERTY_NAME, nodeWithList, false);
    return nodeWithList;
  }

  function getClientList(): string[] {
    return element[MODEL_PROPERTY_NAME][LIST_PROPERTY_NAME];
  }

  // Binds, then adds a fresh set of raw items and asserts each addition produced
  // a separate splice(path, start, deleteCount, item) call. Mirrors
  // assertUpdateListValues, shared by testUpdateList and testUpdateBasicTypeList.
  function assertUpdateListValues(nodeWithList: StateNode): void {
    bind(node, element);
    Reactive.flush();

    const newList = ['1', '2', '3'];
    fillNodeWithListItems(nodeWithList, newList);
    Reactive.flush();

    // Since fillNodeWithListItems makes a separate add call for every element in
    // newList, there are the same number of splice calls.
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

  // addMockMethods: give the element a _propertiesChanged to wrap and a ready
  // hook, and count the calls so the wrapping can be asserted.
  function addMockMethods(): void {
    element.propertiesChangedCallCount = 0;
    element._propertiesChanged = (): void => {
      element.propertiesChangedCallCount += 1;
    };
  }

  // emulatePolymerPropertyChange: what Polymer calls when a property changed on
  // the client side.
  function emulatePolymerPropertyChange(propertyName: string, newValue: unknown): void {
    element._propertiesChanged({}, { [propertyName]: newValue }, {});
  }

  // Reads a sub-property from the client-side model object; mirrors the nested
  // WidgetUtil.getJsProperty(getJsProperty(element, MODEL), subProperty) reads.
  function getClientSubProperty(subProperty: string): unknown {
    return element[MODEL_PROPERTY_NAME][subProperty];
  }

  it('adds a model property to the element', () => {
    // Ported from testPropertyAdded.
    bind(node, element);
    const propertyName = 'black';
    const propertyValue = 'coffee';

    setModelProperty(node, propertyName, propertyValue, true);

    expect(element[propertyName]).to.equal(propertyValue);
  });

  it('updates a model property on the element', () => {
    // Ported from testPropertyUpdated.
    bind(node, element);
    const propertyName = 'black';
    setModelProperty(node, propertyName, 'coffee', true);
    const newValue = 'tea';

    setModelProperty(node, propertyName, newValue, true);

    expect(element[propertyName]).to.equal(newValue);
  });

  it('does not update a model property after the node is unregistered', () => {
    // Ported from testUnregister.
    bind(node, element);
    const propertyName = 'black';
    const propertyValue = 'coffee';
    setModelProperty(node, propertyName, propertyValue, true);

    harness.tree.unregisterNode(node);
    setModelProperty(node, propertyName, 'bubblegum', true);

    expect(element[propertyName]).to.equal(propertyValue);
  });

  it('sets a model sub-property on the element', () => {
    // Ported from testSetSubProperty.
    const subProperty = 'subProp';
    const value = 'foo';
    setModelProperty(modelNode, subProperty, value, false);

    bind(node, element);
    Reactive.flush();

    expect(getClientSubProperty(subProperty)).to.equal(value);
  });

  it('updates a model sub-property on the element', () => {
    // Ported from testUpdateSubProperty.
    bind(node, element);

    const subProperty = 'subProp';
    setModelProperty(modelNode, subProperty, 'foo', true);

    const newValue = 'bar';
    setModelProperty(modelNode, subProperty, newValue, true);

    expect(getClientSubProperty(subProperty)).to.equal(newValue);
  });

  it('does not update a model sub-property after the nodes are unregistered', () => {
    // Ported from testSubPropertyUnregister.
    bind(node, element);

    const subProperty = 'subProp';
    const value = 'foo';
    setModelProperty(modelNode, subProperty, value, true);
    harness.tree.unregisterNode(node);
    harness.tree.unregisterNode(modelNode);

    setModelProperty(modelNode, subProperty, 'bar', true);

    expect(getClientSubProperty(subProperty)).to.equal(value);
  });

  it('syncs an updatable model property changed on the client', async () => {
    // Ported from testInitialUpdateModelProperty_propertyIsUpdatable_propertyIsSynced.
    addMockMethods();
    const propertyName = 'black';
    const propertyValue = 'coffee';
    setModelProperty(node, propertyName, propertyValue, false);

    node.setNodeData(new UpdatableModelProperties([propertyName]));

    bind(node, element);
    Reactive.flush();
    expect(
      element[propertyName],
      `Expected to have property with name ${propertyName} defined after initial binding`
    ).to.equal(propertyValue);

    // Let the deferred initial update run, as the GWT test's CustomScheduler does.
    await new Promise((resolve) => {
      setTimeout(resolve, 0);
    });

    const newPropertyValue = 'bubblegum';
    emulatePolymerPropertyChange(propertyName, newPropertyValue);
    Reactive.flush();

    expect(
      element[propertyName],
      `Expected to have property with name ${propertyName} updated from client side`
    ).to.equal(newPropertyValue);
    expect(node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(propertyName).getValue()).to.equal(
      newPropertyValue
    );
    expect(harness.synchronizedProperties.get(node)?.get(propertyName)).to.equal(newPropertyValue);
  });

  it('does not sync an updatable model property while the initial update is pending', () => {
    // Ported from testInitialUpdateModelProperty_propertyIsUpdatableAndSchedulerIsNotExecuted_propertyIsNotSync.
    // The deferred initial update is scheduled but never awaited here, which is
    // what the GWT test achieves with a scheduler that drops deferred commands.
    addMockMethods();
    const propertyName = 'black';
    const propertyValue = 'coffee';
    setModelProperty(node, propertyName, propertyValue, false);

    node.setNodeData(new UpdatableModelProperties([propertyName]));

    bind(node, element);
    Reactive.flush();
    expect(element[propertyName]).to.equal(propertyValue);

    emulatePolymerPropertyChange(propertyName, 'bubblegum');
    Reactive.flush();

    expect(element[propertyName]).to.equal(propertyValue);
    expect(node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(propertyName).getValue()).to.equal(propertyValue);
    expect(harness.synchronizedProperties.has(node)).to.be.false;
  });

  it('syncs an updatable model sub-property changed on the client', async () => {
    // Ported from testUpdateModelSubProperty_subpropertyIsUpdatableAndIsNotSetFromServer_subpropertyIsSync.
    addMockMethods();
    const subModelNode = createAndAttachModelNode('bar');

    node.setNodeData(new UpdatableModelProperties(['bar.foo']));

    bind(node, element);
    Reactive.flush();
    await new Promise((resolve) => {
      setTimeout(resolve, 0);
    });

    const newSubPropertyValue = 'baz';
    setProperty(element, 'bar.foo', newSubPropertyValue);
    emulatePolymerPropertyChange('bar.foo', newSubPropertyValue);
    Reactive.flush();

    expect(
      element.bar.foo,
      "Expected to have an object 'bar' with a property named 'foo' updated from client side"
    ).to.equal(newSubPropertyValue);
    expect(subModelNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo').getValue()).to.equal(
      newSubPropertyValue
    );
    expect(harness.synchronizedProperties.get(subModelNode)?.get('foo')).to.equal(newSubPropertyValue);
  });

  it('does not sync a model property that is not updatable', async () => {
    // Ported from testUpdateModelProperty_propertyIsNotUpdatable_propertyIsNotSync.
    addMockMethods();
    const propertyName = 'black';
    const propertyValue = 'coffee';
    setModelProperty(node, propertyName, propertyValue, false);

    bind(node, element);
    Reactive.flush();
    await new Promise((resolve) => {
      setTimeout(resolve, 0);
    });
    expect(element[propertyName]).to.equal(propertyValue);

    emulatePolymerPropertyChange(propertyName, 'doesNotMatter');
    Reactive.flush();

    expect(
      element[propertyName],
      `Expected the property with name ${propertyName} not to be updated since it is not updatable`
    ).to.equal(propertyValue);
    expect(harness.synchronizedProperties.has(node)).to.be.false;
  });

  it('binds a Polymer element that is defined only after the initial binding', async () => {
    // Ported from testLatePolymerInit.
    const propertyName = 'black';
    const propertyValue = 'coffee';

    // emulatePolymerNotLoaded: the element must not look like a Polymer element
    // when bind() runs, so the strategy takes the customElements.whenDefined path.
    const oldPolymer = (window as any).Polymer;
    (window as any).Polymer = null;
    Object.defineProperty(element, 'constructor', { value: {}, configurable: true });

    // addMockMethods: count _propertiesChanged calls, and resolve whenDefined
    // once, turning the element into a Polymer element as Polymer would.
    element.propertiesChangedCallCount = 0;
    element._propertiesChanged = (): void => {
      element.propertiesChangedCallCount += 1;
    };
    element.callbackCallCount = 0;
    const originalWhenDefined = window.customElements.whenDefined.bind(window.customElements);
    (window.customElements as any).whenDefined = () =>
      new Promise<void>((resolve) => {
        (window as any).Polymer = oldPolymer;
        Object.defineProperty(element, 'constructor', {
          value: { polymerElementVersion: '2.0.1' },
          configurable: true
        });
        element.callbackCallCount += 1;
        resolve();
      });

    try {
      setModelProperty(node, propertyName, propertyValue, false);
      node.setNodeData(new UpdatableModelProperties([propertyName]));

      bind(node, element);
      Reactive.flush();
      // let the whenDefined promise race settle so the late hook-up runs
      await new Promise((resolve) => setTimeout(resolve, 0));
      Reactive.flush();

      expect(element[propertyName]).to.equal(propertyValue);

      const newPropertyValue = 'bubblegum';
      // emulatePolymerPropertyChange
      element._propertiesChanged({}, { [propertyName]: newPropertyValue }, {});
      Reactive.flush();

      expect(element[propertyName]).to.equal(newPropertyValue);
      expect(node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(propertyName).getValue()).to.equal(
        newPropertyValue
      );
      expect(harness.synchronizedProperties.get(node)?.get(propertyName)).to.equal(newPropertyValue);
      expect(element.propertiesChangedCallCount, '_propertiesChanged should be triggered exactly once').to.equal(1);
      expect(
        element.callbackCallCount,
        'exactly one whenDefined.then callback should run after the element was initialized'
      ).to.equal(1);
    } finally {
      (window.customElements as any).whenDefined = originalWhenDefined;
      (window as any).Polymer = oldPolymer;
    }
  });

  it('adds a model list to the element', () => {
    // Ported from testAddList.
    const serverList = ['one', 'two'];
    createAndAttachNodeWithList(modelNode, serverList);

    bind(node, element);
    Reactive.flush();

    expect(getClientList()).to.deep.equal(serverList);
  });

  it('adds a basic-type model list to the element', () => {
    // Ported from testAddBasicTypeList: the list items are BASIC_TYPE_VALUE nodes
    // rather than raw values, but the client list is the same.
    const serverList = ['one', 'two'];
    createAndAttachNodeWithList(modelNode, serverList, createBasicTypeWrapper);

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

  it('replaces the basic-type model list when a new list node is set for the same property', () => {
    // Ported from testSetNewBasicTypeListForTheSameProperty.
    createAndAttachNodeWithList(modelNode, ['one', 'two'], createBasicTypeWrapper);

    bind(node, element);
    Reactive.flush();

    const newServerList = ['1', '2', '3'];
    createAndAttachNodeWithList(modelNode, newServerList, createBasicTypeWrapper);
    Reactive.flush();

    expect(getClientList()).to.deep.equal(newServerList);
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

  it('pushes each list addition to the element via splice', () => {
    // Ported from testUpdateList.
    const nodeWithList = createAndAttachNodeWithList(modelNode, ['one', 'two']);

    assertUpdateListValues(nodeWithList);
  });

  it('pushes each basic-type list addition to the element via splice', () => {
    // Ported from testUpdateBasicTypeList: the initial list is built from
    // BASIC_TYPE_VALUE nodes, but the splice updates behave the same.
    const nodeWithList = createAndAttachNodeWithList(modelNode, ['one', 'two'], createBasicTypeWrapper);

    assertUpdateListValues(nodeWithList);
  });

  // Beyond the Java suite: the GWT suite has no test that drives a property
  // change of a dom-repeat item, so this covers the item-change bridge the
  // strategy installs on the dom-repeat prototype.
  describe('beyond the Java suite', () => {
    it('syncs a changed dom-repeat item property to the server', () => {
      // The item node is a child of the bound node, which is what the host
      // check in the item-change handler verifies.
      const itemNode = new StateNode(nextId++, harness.tree);
      harness.tree.registerNode(itemNode);
      itemNode.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('value').setValue('old');
      itemNode.setParent(node);

      // A minimal dom-repeat whose prototype the strategy replaces, with the
      // __dataHost chain Polymer maintains from the dom-repeat to its template.
      class FakeDomRepeat {
        __dataHost: unknown = element;

        _propertiesChanged(_currentProps: unknown, _changedProps: unknown, _oldProps: unknown): void {}
      }
      const domRepeat = new FakeDomRepeat();
      element.root = { querySelector: (selector: string) => (selector === 'dom-repeat' ? domRepeat : null) };
      element.ready = (): void => {};

      bind(node, element);
      Reactive.flush();

      // Polymer calls ready() once the local DOM is ready; that is where the
      // dom-repeat bridge is installed.
      element.ready();

      domRepeat._propertiesChanged(
        { items: [{ nodeId: itemNode.getId(), value: 'new' }] },
        { 'items.0.value': 'new' },
        {}
      );

      expect(harness.synchronizedProperties.get(itemNode)?.get('value')).to.equal('new');
    });
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
