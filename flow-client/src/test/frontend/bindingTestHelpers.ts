// Shared full-state-tree harness for the binding tests ported from the GWT
// integration tests (GwtMultipleBindingTest, GwtBasicElementBinderTest,
// GwtPropertyElementBinderTest, GwtPolymerModelTest). These GWT tests bind a
// real StateNode to a real DOM element via the real Binder over a real
// StateTree, so this harness builds that same stack instead of the per-function
// fakes used by the unit-level binding tests.

import { bind } from '../../main/frontend/internal/binding/Binder';
import { ConstantPool } from '../../main/frontend/internal/ConstantPool';
import { StateNode } from '../../main/frontend/internal/StateNode';
import { StateTree } from '../../main/frontend/internal/StateTree';
import { NodeFeatures, NodeProperties } from '../../main/frontend/internal/nodefeature/NodeFeatures';

// Arguments collected by the sendExistingElementWithIdAttachToServer RPC; mirrors
// CollectingStateTree.existingElementRpcArgs in GwtPropertyElementBinderTest.
export type ExistingElementRpcArg = StateNode | number | string | null;

export interface CollectingTree {
  tree: StateTree;
  constantPool: ConstantPool;
  // sendEventToServer collected (node + data), mirrors collectedNodes/collectedEventData
  collectedNodes: StateNode[];
  collectedEventData: unknown[];
  // sendNodePropertySyncToServer collected per node -> name -> value
  synchronizedProperties: Map<StateNode, Map<string, unknown>>;
  existingElementRpcArgs: ExistingElementRpcArg[];
  clearSynchronizedProperties(): void;
}

// Builds a StateTree whose registry satisfies both the slice StateTree needs
// (getInitialPropertiesHandler / getServerConnector) and the slice the binding
// strategy reaches for (getConstantPool / getExistingElementMap /
// getApplicationConfiguration). The server-facing calls are collected so the
// ported tests can assert on them.
export function makeCollectingTree(): CollectingTree {
  const constantPool = new ConstantPool();
  const existingElementMap = {
    getElement: (): Element | null => null,
    remove: (): void => {}
  };
  const applicationConfiguration = {
    isWebComponentMode: (): boolean => false,
    getServiceUrl: (): string => ''
  };

  const collectedNodes: StateNode[] = [];
  const collectedEventData: unknown[] = [];
  const synchronizedProperties = new Map<StateNode, Map<string, unknown>>();
  const existingElementRpcArgs: ExistingElementRpcArg[] = [];

  const initialPropertiesHandler = {
    flushPropertyUpdates: (): void => {},
    nodeRegistered: (): void => {},
    handlePropertyUpdate: (): boolean => false
  };

  const serverConnector = {
    sendEventMessage: (node: StateNode, _eventType: string, eventData: unknown): void => {
      collectedNodes.push(node);
      collectedEventData.push(eventData);
    },
    sendNodeSyncMessage: (node: StateNode, _mapId: number, name: string, value: unknown): void => {
      let nodeMap = synchronizedProperties.get(node);
      if (nodeMap === undefined) {
        nodeMap = new Map();
        synchronizedProperties.set(node, nodeMap);
      }
      nodeMap.set(name, value);
    },
    sendTemplateEventMessage: (): void => {},
    sendExistingElementAttachToServer: (): void => {},
    sendExistingElementWithIdAttachToServer: (
      parent: StateNode,
      requestedId: number,
      assignedId: number,
      id: string | null
    ): void => {
      existingElementRpcArgs.push(parent, requestedId, assignedId, id);
    }
  };

  const registry: any = {
    getInitialPropertiesHandler: () => initialPropertiesHandler,
    getServerConnector: () => serverConnector,
    getConstantPool: () => constantPool,
    getExistingElementMap: () => existingElementMap,
    getApplicationConfiguration: () => applicationConfiguration
  };

  const tree = new StateTree(registry);

  return {
    tree,
    constantPool,
    collectedNodes,
    collectedEventData,
    synchronizedProperties,
    existingElementRpcArgs,
    clearSynchronizedProperties: () => synchronizedProperties.clear()
  };
}

// A StateNode that fails an assertion if a feature other than ELEMENT_DATA is
// accessed after it is marked bound. Mirrors GwtMultipleBindingTest.TestStateNode:
// it lets the double-bind tests prove the second Binder.bind() is a no-op that
// never touches the node's features again.
export class BindGuardStateNode extends StateNode {
  private bound = false;
  private readonly onViolation: (message: string) => void;

  constructor(id: number, tree: StateTree, onViolation: (message: string) => void) {
    super(id, tree);
    this.onViolation = onViolation;
  }

  setBound(): void {
    this.bound = true;
  }

  override getList(id: number): ReturnType<StateNode['getList']> {
    if (this.bound) {
      this.onViolation(`getList(${id}) accessed after the node was bound`);
    }
    return super.getList(id);
  }

  override getMap(id: number): ReturnType<StateNode['getMap']> {
    if (this.bound && id !== NodeFeatures.ELEMENT_DATA) {
      this.onViolation(`getMap(${id}) accessed after the node was bound`);
    }
    return super.getMap(id);
  }
}

export { bind, StateNode, NodeFeatures, NodeProperties };
