// A full ConnectionStateHandler for the suites that pass one but only care about
// a few of its methods. Kept in one module so a member added to the interface is
// filled in once rather than in every suite.

import type { ConnectionStateHandler } from '../../../../main/frontend/internal/client/communication/ConnectionStateHandler';

/** A handler whose members are all present and do nothing, except the overrides. */
export function fakeConnectionStateHandler(overrides: Partial<ConnectionStateHandler> = {}): ConnectionStateHandler {
  return {
    heartbeatException: () => {},
    heartbeatInvalidStatusCode: () => {},
    heartbeatOk: () => {},
    pushClosed: () => {},
    pushClientTimeout: () => {},
    pushError: () => {},
    pushReconnectPending: () => {},
    pushOk: () => {},
    pushScriptLoadError: () => {},
    xhrException: () => {},
    xhrInvalidContent: () => {},
    xhrInvalidStatusCode: () => {},
    xhrOk: () => {},
    pushNotConnected: () => {},
    pushInvalidContent: () => {},
    configurationUpdated: () => {},
    ...overrides
  };
}
