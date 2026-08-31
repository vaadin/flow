// Beyond the Java suite: XhrConnectionError has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import { XhrConnectionError } from '../../../../../main/frontend/internal/client/communication/XhrConnectionError';

describe('XhrConnectionError', () => {
  it('exposes the xhr, payload and exception', () => {
    const xhr = new XMLHttpRequest();
    const payload = { rpc: [] };
    const error = new Error('boom');
    const connectionError = new XhrConnectionError(xhr, payload, error);
    expect(connectionError.getXhr()).to.equal(xhr);
    expect(connectionError.getPayload()).to.equal(payload);
    expect(connectionError.getException()).to.equal(error);

    expect(new XhrConnectionError(xhr, payload, null).getException()).to.equal(null);
  });
});
