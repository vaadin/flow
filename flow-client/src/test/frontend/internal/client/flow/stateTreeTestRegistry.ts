// Shared registry fakes for the suites that need a StateTree but never reach
// through it to the server or the binding layer. Kept in one module so a member
// added to Registry is filled in once rather than in every suite.

import type { Registry } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { ConstantPool } from '../../../../../main/frontend/internal/client/flow/ConstantPool';
import { ExistingElementMap } from '../../../../../main/frontend/internal/client/ExistingElementMap';

/** A registry whose members are all present but inert. */
export function inertRegistry(): Registry {
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
    getConstantPool: () => new ConstantPool(),
    getExistingElementMap: () => new ExistingElementMap()
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
