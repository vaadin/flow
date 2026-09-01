// Shared registry fakes for the suites that need a StateTree but never reach
// through it to the server or the binding layer. Kept in one module so a member
// added to Registry is filled in once rather than in every suite.

import type { UILifecycle } from '../../../../../main/frontend/internal/client/UILifecycle';
import { type TestRegistry, type TestServices, testRegistry } from '../testRegistry';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { ConstantPool } from '../../../../../main/frontend/internal/client/flow/ConstantPool';
import { ApplicationConfiguration } from '../../../../../main/frontend/internal/client/ApplicationConfiguration';
import { ExistingElementMap } from '../../../../../main/frontend/internal/client/ExistingElementMap';

// Everything a state tree needs except the server connector, which a recording
// registry replaces. One instance each per registry: the binding path reads these
// through several calls, so handing out a fresh one per call would hide anything
// written through an earlier call. ApplicationConfiguration, ConstantPool and
// ExistingElementMap are the real classes, which need nothing else.
function inertServices(): TestServices {
  return {
    InitialPropertiesHandler: {
      flushPropertyUpdates: () => {},
      nodeRegistered: () => {},
      handlePropertyUpdate: () => false
    },
    ApplicationConfiguration: new ApplicationConfiguration(),
    ConstantPool: new ConstantPool(),
    ExistingElementMap: new ExistingElementMap()
  };
}

/** A registry whose members are all present but inert. */
export function inertRegistry(): TestRegistry {
  return testRegistry({
    ...inertServices(),
    ServerConnector: {
      sendEventMessage: () => {},
      sendNodeSyncMessage: () => {},
      sendTemplateEventMessage: () => {},
      sendExistingElementAttachToServer: () => {},
      sendExistingElementWithIdAttachToServer: () => {},
      sendReturnChannelMessage: () => {}
    }
  });
}

/** What a recording registry collected from the server-facing calls. */
export interface RecordedCalls {
  // Per node id, the property name -> value pairs sent by sendNodeSyncMessage.
  syncs: Map<number, Map<string, unknown>>;
  // The arguments of each sendExistingElementAttachToServer call.
  existingElementAttaches: Array<{ nodeId: number; id: number; existingId: number; tagName: string; index: number }>;
  // The arguments of each sendReturnChannelMessage call.
  returnChannelMessages: Array<{ nodeId: number; channelId: number; args: unknown[] }>;
  // The arguments of each sendEventMessage call.
  events: Array<{ nodeId: number; eventType: string; eventData: unknown }>;
}

/**
 * A registry whose members are inert apart from the server connector, which
 * records what was sent, for suites that assert on the round trip.
 */
export function recordingRegistry(): { registry: TestRegistry; recorded: RecordedCalls } {
  const recorded: RecordedCalls = {
    syncs: new Map(),
    existingElementAttaches: [],
    returnChannelMessages: [],
    events: []
  };
  // The recording connector replaces the inert one, so the registry is built from
  // the shared services rather than from inertRegistry: a service is registered
  // once.
  const registry = testRegistry(inertServices());
  registry.register('ServerConnector', {
    sendTemplateEventMessage: () => {},
    sendExistingElementWithIdAttachToServer: () => {},
    sendEventMessage: (node: StateNode, eventType: string, eventData: unknown) => {
      recorded.events.push({ nodeId: node.getId(), eventType, eventData });
    },
    sendReturnChannelMessage: (nodeId: number, channelId: number, args: unknown[]) => {
      recorded.returnChannelMessages.push({ nodeId, channelId, args });
    },
    sendNodeSyncMessage: (node: StateNode, _featureId: number, name: string, value: unknown) => {
      const byName = recorded.syncs.get(node.getId()) ?? new Map<string, unknown>();
      byName.set(name, value);
      recorded.syncs.set(node.getId(), byName);
    },
    // eslint-disable-next-line @typescript-eslint/max-params -- mirrors the ServerConnector signature
    sendExistingElementAttachToServer: (
      node: { getId(): number },
      id: number,
      existingId: number,
      tagName: string,
      index: number
    ) => {
      recorded.existingElementAttaches.push({ nodeId: node.getId(), id, existingId, tagName, index });
    }
  });
  return { registry, recorded };
}

/**
 * A registry whose every member throws: for suites that must fail loudly if the
 * code under test reaches the registry at all.
 */
export function unavailableRegistry(): TestRegistry {
  // Nothing registered: the registry itself throws on any lookup.
  return testRegistry({});
}

/**
 * A registered node on an inert tree, for the suites that only need a node whose
 * maps fire real reactive change events.
 */
export function inertNode(id = 2): StateNode {
  const tree = new StateTree(inertRegistry());
  const node = new StateNode(id, tree);
  tree.registerNode(node);
  return node;
}

/**
 * The stateless members every hand-wired registry needs, for the suites that
 * assemble real services in one registry the way DefaultRegistry will. The
 * caller adds the services it drives, and the UI is already running.
 *
 * @param uiLifecycle - the lifecycle to expose, already in the state the suite needs
 * @returns the registry, to be extended with the services under test
 */

export function wiredRegistryBase(uiLifecycle: UILifecycle): TestRegistry {
  return testRegistry({ ...inertServices(), UILifecycle: uiLifecycle });
}
