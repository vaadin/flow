import { ConstantPool } from '../../../../../main/frontend/internal/client/flow/ConstantPool';
import { ExistingElementMap } from '../../../../../main/frontend/internal/client/ExistingElementMap';
import { expect } from '@open-wc/testing';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import type { NodeUnregisterEvent } from '../../../../../main/frontend/internal/client/flow/NodeUnregisterEvent';
import { StateTree, type Registry } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { bind } from '../../../../../main/frontend/internal/client/flow/binding/Binder';
import { get as getServerEventObject } from '../../../../../main/frontend/internal/client/flow/binding/ServerEventObject';
import { JsonConstants } from '../../../../../main/frontend/internal/flow/shared/JsonConstants';
import { ApplicationConfiguration } from '../../../../../main/frontend/internal/client/ApplicationConfiguration';
import { InitialPropertiesHandler } from '../../../../../main/frontend/internal/client/InitialPropertiesHandler';
import { Reactive } from '../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';

interface Sync {
  node: StateNode;
  mapId: number;
  name: string;
  value: unknown;
}

interface TemplateEvent {
  node: StateNode;
  methodName: string;
  args: unknown[];
  promiseId: number;
}

// The real handler, with the three members these cases observe overridden; the
// class is ported, so a stand-in would no longer satisfy the registry.
class RecordingInitialPropertiesHandler extends InitialPropertiesHandler {
  flushCount = 0;

  readonly registeredNodes: StateNode[] = [];

  readonly #propertyUpdateResult: boolean;

  constructor(propertyUpdateResult: boolean) {
    super({
      getStateTree: () => {
        throw new Error('state tree not available in this test');
      }
    });
    this.#propertyUpdateResult = propertyUpdateResult;
  }

  override flushPropertyUpdates(): void {
    this.flushCount++;
  }

  override nodeRegistered(node: StateNode): void {
    this.registeredNodes.push(node);
  }

  override handlePropertyUpdate(): boolean {
    return this.#propertyUpdateResult;
  }
}

function makeTree(handlePropertyUpdateResult = false): {
  tree: StateTree;
  syncs: Sync[];
  templateEvents: TemplateEvent[];
  getFlushCount: () => number;
  getRegisteredNodes: () => StateNode[];
} {
  const syncs: Sync[] = [];
  const templateEvents: TemplateEvent[] = [];
  const initialPropertiesHandler = new RecordingInitialPropertiesHandler(handlePropertyUpdateResult);
  // One instance each: the code under test reads these through several calls,
  // so a fresh instance per call would hide anything written by an earlier one.
  const constantPool = new ConstantPool();
  const existingElementMap = new ExistingElementMap();
  const registry: Registry = {
    getInitialPropertiesHandler: () => initialPropertiesHandler,
    getServerConnector: () => ({
      sendEventMessage: () => {},
      sendNodeSyncMessage: (node, mapId, name, value) => syncs.push({ node, mapId, name, value }),
      sendTemplateEventMessage: (node, methodName, args, promiseId) =>
        templateEvents.push({ node, methodName, args, promiseId }),
      sendExistingElementAttachToServer: () => {},
      sendExistingElementWithIdAttachToServer: () => {},
      sendReturnChannelMessage: () => {}
    }),
    getApplicationConfiguration: () => new ApplicationConfiguration(),
    getConstantPool: () => constantPool,
    getExistingElementMap: () => existingElementMap
  };
  return {
    tree: new StateTree(registry),
    syncs,
    templateEvents,
    getFlushCount: () => initialPropertiesHandler.flushCount,
    getRegisteredNodes: () => initialPropertiesHandler.registeredNodes
  };
}

function setVisible(node: StateNode, value: boolean): void {
  node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE).setValue(value);
}

// Ported from com.vaadin.client.flow.StateTreeTest and
// com.vaadin.client.flow.GwtStateTreeTest.
describe('StateTree', () => {
  it('maps registered nodes by id', () => {
    // Ported from testIdMappings.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);

    expect(tree.getNode(node.getId())).to.equal(null);

    tree.registerNode(node);

    expect(tree.getNode(node.getId())).to.equal(node);
  });

  it('throws when registering an already-registered node', () => {
    // Ported from testRegisterExistingThrows.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(() => tree.registerNode(node)).to.throw();
  });

  it('throws when registering a null node', () => {
    // Ported from testRegisterNullThrows.
    // testRegisterNullThrows: registering null is rejected. Java asserts an
    // AssertionError via `assert node != null`; TypeScript reaches the argument
    // with a cast and registration throws before completing.
    const { tree } = makeTree();
    expect(() => tree.registerNode(null as unknown as StateNode)).to.throw();
  });

  it('fires the unregister event exactly once with the right node', () => {
    // Ported from testNodeUnregister.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(node.isUnregistered()).to.equal(false);

    let lastEvent: NodeUnregisterEvent | null = null;
    node.addUnregisterListener((event) => {
      expect(lastEvent, 'Unexpected event fired').to.equal(null);
      lastEvent = event;
    });

    tree.unregisterNode(node);

    expect(lastEvent).to.not.equal(null);
    expect(lastEvent!.getNode()).to.equal(node);
    expect(node.isUnregistered()).to.equal(true);
    expect(tree.getNode(node.getId())).to.equal(null);
  });

  it('does not fire a removed unregister listener', () => {
    // Ported from testRemoveUnregisterListener.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);

    const remover = node.addUnregisterListener(() => expect.fail('Should never run'));
    remover.remove();

    tree.unregisterNode(node);
  });

  it('throws when unregistering a node that was never registered', () => {
    // Ported from unregisterNonRegisteredNode.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    expect(() => tree.unregisterNode(node)).to.throw();
  });

  it('throws when unregistering a node twice', () => {
    // Ported from unregisterUnregisteredNode.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    tree.unregisterNode(node);
    // Should run fine up to this point
    expect(() => tree.unregisterNode(node)).to.throw();
  });

  it('throws when the binder runs while a tree update is in progress', () => {
    // Ported from testUpdatingTree_triggeringBinder_causesAssertionError.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    tree.setUpdateInProgress(true);

    // Asserting on the message matters here: a bare node has no applicable
    // binding strategy either, so a bare throw() would pass even if the
    // update-in-progress assert were gone.
    expect(() => bind(node, null as unknown as Node)).to.throw(
      'Binding state node while processing state tree changes'
    );
  });

  describe('sendNodePropertySyncToServer', () => {
    it('sends a non-initial property of a valid node', () => {
      // Ported from sendNodePropertySyncToServer_notInitialProperty_propertyIsSent.
      const { tree, syncs } = makeTree(false);
      const property = tree.getRootNode().getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(1);
      expect(syncs[0].name).to.equal('foo');
      expect(syncs[0].value).to.equal('bar');
      expect(syncs[0].node).to.equal(tree.getRootNode());
    });

    it('does not send a property of a detached node', () => {
      // Ported from sendNodePropertySyncToServer_nodeDetached_propertyNotIsSent.
      const { tree, syncs } = makeTree(false);
      const node = new StateNode(7, tree);
      tree.registerNode(node);
      const property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.unregisterNode(node);
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(0);
    });

    it('does not send an initial property', () => {
      // Ported from sendNodePropertySyncToServer_initialProperty_propertyIsNoSent.
      const { tree, syncs } = makeTree(true);
      const property = tree.getRootNode().getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(0);
    });
  });

  it('setUpdateInProgress flushes property updates', () => {
    // Ported from setUpdateInProgress_flushPropertyUpdates.
    // One Java @Test asserts the flush fires once after setUpdateInProgress(true)
    // and again (twice total) after setUpdateInProgress(false); kept as a single
    // it() so the case maps 1:1 to the Java method.
    const { tree, getFlushCount } = makeTree();
    expect(getFlushCount()).to.equal(0);
    tree.setUpdateInProgress(true);
    expect(tree.isUpdateInProgress()).to.equal(true);
    expect(getFlushCount()).to.equal(1);
    tree.setUpdateInProgress(false);
    expect(getFlushCount()).to.equal(2);
  });

  it('does not call the property handler when registering while no update is in progress', () => {
    // Ported from registerNode_updateIsNotInProgress_noPropertyHandlerCalls.
    const { tree, getFlushCount, getRegisteredNodes } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(getFlushCount()).to.equal(0);
    expect(getRegisteredNodes()).to.deep.equal([]);
  });

  it('notifies the property handler when registering while an update is in progress', () => {
    // Ported from registerNode_updateIsInProgress_noPropertyHandlerCalls.
    const { tree, getRegisteredNodes } = makeTree();
    tree.setUpdateInProgress(true);
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(getRegisteredNodes()).to.deep.equal([node]);
  });

  describe('isVisible', () => {
    it('is true when the node has no element-data feature', () => {
      // Ported from isVisible_nodeHasNoFeature_nodeIsVisible.
      const { tree } = makeTree();
      expect(tree.isVisible(tree.getRootNode())).to.equal(true);
    });

    it('is true when visible is explicitly true', () => {
      // Ported from isVisible_nodeHasFeatureAndVisibleValue_nodeIsVisible.
      const { tree } = makeTree();
      setVisible(tree.getRootNode(), true);
      expect(tree.isVisible(tree.getRootNode())).to.equal(true);
    });

    it('is true when the feature exists but has no value', () => {
      // Ported from isVisible_nodeHasFeatureAndNoValue_nodeIsVisible.
      const { tree } = makeTree();
      tree.getRootNode().getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE);
      expect(tree.isVisible(tree.getRootNode())).to.equal(true);
    });

    it('is false when visible is explicitly false', () => {
      // Ported from isVisible_nodeHasFeatureAndNotVisibleValue_nodeIsNotVisible.
      const { tree } = makeTree();
      setVisible(tree.getRootNode(), false);
      expect(tree.isVisible(tree.getRootNode())).to.equal(false);
    });
  });

  describe('isActive', () => {
    it('is true for a visible node without a parent', () => {
      // Ported from isActive_nodeIsVisibleAndNoParent_nodeIsActive.
      const { tree } = makeTree();
      const node = new StateNode(2, tree);
      expect(tree.isActive(node)).to.equal(true);
    });

    it('is false for an invisible node without a parent', () => {
      // Ported from isActive_nodeIsInvisibleAndNoParent_nodeIsActive.
      const { tree } = makeTree();
      const node = new StateNode(2, tree);
      setVisible(node, false);
      expect(tree.isActive(node)).to.equal(false);
    });

    it('is true for a visible node with a visible parent', () => {
      // Ported from isActive_nodeIsVisibleAndVisibleParent_nodeIsActive.
      const { tree } = makeTree();
      const parent = new StateNode(2, tree);
      const node = new StateNode(3, tree);
      node.setParent(parent);
      expect(tree.isActive(node)).to.equal(true);
    });

    it('is false for a visible node with an invisible parent', () => {
      // Ported from isActive_nodeIsVisibleAndInvisibleParent_nodeIsNotActive.
      const { tree } = makeTree();
      const parent = new StateNode(2, tree);
      setVisible(parent, false);
      const node = new StateNode(3, tree);
      node.setParent(parent);
      expect(tree.isActive(node)).to.equal(false);
    });
  });

  it('prepareForResync leaves only the root registered', () => {
    // Ported from treeHasChildren_prepareForResync_onlyRootRemainsRegistered.
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);

    tree.prepareForResync();

    expect(tree.getRootNode().isUnregistered()).to.equal(false);
    expect(node.isUnregistered()).to.equal(true);
    expect(tree.isResync()).to.equal(true);
  });

  // Cases from the GWT-side counterpart GwtStateTreeTest; its four test* methods
  // run under GWTTestCase and are ported 1:1 here.
  describe('GwtStateTreeTest', () => {
    it('delegates a template event to the server connector', () => {
      // Beyond the Java suite.
      // testSendTemplateEventToServer_delegateToServerConnector
      const { tree, templateEvents } = makeTree();
      const node = new StateNode(2, tree);
      tree.registerNode(node);
      const args = [true, 'bar', 46.2];

      tree.sendTemplateEventToServer(node, 'foo', args, -1);

      expect(templateEvents.length).to.equal(1);
      expect(templateEvents[0].node).to.equal(node);
      expect(templateEvents[0].methodName).to.equal('foo');
      // Java casts argsArray with crazyJsCast (erased in TS), so the array is
      // passed straight through to the connector.
      expect(templateEvents[0].args).to.equal(args);
      expect(templateEvents[0].promiseId).to.equal(-1);
    });

    it('ignores a deferred template event once the node is unregistered', () => {
      // Beyond the Java suite.
      // testDeferredTemplateMessage_isIgnored
      const { tree, templateEvents } = makeTree();
      const node = new StateNode(2, tree);
      tree.registerNode(node);

      Reactive.addPostFlushListener(() => {
        tree.sendTemplateEventToServer(node, 'click', [], -1);
        expect(templateEvents.length, 'message should not have been sent').to.equal(0);
      });

      tree.unregisterNode(node);
      Reactive.flush();

      expect(templateEvents.length).to.equal(0);
    });

    it('unregisters descendants and clears the root child lists on resync', () => {
      // Ported from GwtStateTreeTest.testPrepareForResync_unregistersDescendantsAndClearsRootChildren.
      // testPrepareForResync_unregistersDescendantsAndClearsRootChildren
      const { tree } = makeTree();
      const root = tree.getRootNode();

      const child = new StateNode(2, tree);
      child.setParent(root);
      tree.registerNode(child);
      root.getList(NodeFeatures.VIRTUAL_CHILDREN).add(0, child);

      const grandChild = new StateNode(3, tree);
      grandChild.setParent(child);
      tree.registerNode(grandChild);
      child.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, grandChild);

      tree.prepareForResync();

      expect(root.isUnregistered()).to.equal(false);
      expect(root.getList(NodeFeatures.VIRTUAL_CHILDREN).length()).to.equal(0);
      expect(child.isUnregistered()).to.equal(true);
      expect(child.getList(NodeFeatures.ELEMENT_CHILDREN).length()).to.equal(0);
      expect(grandChild.isUnregistered()).to.equal(true);
    });

    it('rejects a pending promise on a descendant during resync', () => {
      // Ported from testPrepareForResync_rejectsPendingPromise.
      const { tree } = makeTree();
      const root = tree.getRootNode();

      const child = new StateNode(2, tree);
      child.setParent(root);
      tree.registerNode(child);
      root.getList(NodeFeatures.VIRTUAL_CHILDREN).add(0, child);

      const element = document.createElement('div');
      child.setDomNode(element);

      // createMockPromise: store a pending promise on the $server object, the
      // way a client-callable method awaiting a server response does.
      const serverObject = getServerEventObject(element) as unknown as Record<string, any>;
      let rejected = false;
      serverObject[JsonConstants.RPC_PROMISE_CALLBACK_NAME].promises[0] = [
        () => {},
        () => {
          rejected = true;
        }
      ];

      tree.prepareForResync();

      expect(rejected).to.equal(true);
    });
  });
});
