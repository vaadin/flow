// Shared registry fakes for the suites that need a StateTree but never reach
// through it to the server or the binding layer. Kept in one module so a member
// added to Registry is filled in once rather than in every suite.

import type { Registry } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { ConstantPool } from '../../../../../main/frontend/internal/client/flow/ConstantPool';
import { ExistingElementMap } from '../../../../../main/frontend/internal/client/ExistingElementMap';

/** A registry whose members are all present but inert. */
export function inertRegistry(): Registry {
  // One instance each per registry: the binding path reads these through
  // several calls, so handing out a fresh one per call would hide anything
  // written through an earlier call.
  const constantPool = new ConstantPool();
  const existingElementMap = new ExistingElementMap();
  return {
    getInitialPropertiesHandler: () => ({
      flushPropertyUpdates: () => {},
      nodeRegistered: () => {},
      handlePropertyUpdate: () => false
    }),
    getServerConnector: () => ({
      sendEventMessage: () => {},
      sendNodeSyncMessage: () => {},
      sendTemplateEventMessage: () => {},
      sendExistingElementAttachToServer: () => {},
      sendExistingElementWithIdAttachToServer: () => {},
      sendReturnChannelMessage: () => {}
    }),
    getApplicationConfiguration: () => ({
      isWebComponentMode: () => false,
      getServiceUrl: () => ''
    }),
    getConstantPool: () => constantPool,
    getExistingElementMap: () => existingElementMap
  };
}

/**
 * A registry whose every member throws: for suites that must fail loudly if the
 * code under test reaches the registry at all.
 */
export function unavailableRegistry(): Registry {
  const unavailable = (): never => {
    throw new Error('registry not available in this test');
  };
  return {
    getInitialPropertiesHandler: unavailable,
    getServerConnector: unavailable,
    getApplicationConfiguration: unavailable,
    getConstantPool: unavailable,
    getExistingElementMap: unavailable
  };
}
