import { expect } from '@open-wc/testing';
import { ReconnectConfiguration } from '../../main/frontend/internal/communication/ReconnectConfiguration';
import { XhrConnectionError } from '../../main/frontend/internal/communication/XhrConnectionError';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';

const RECONNECT_DIALOG_CONFIGURATION = 9;

// A registry whose root node's reconnect-config map returns the given values.
function makeRegistry(values: Record<string, unknown>) {
  const map = {
    getProperty: (key: string) => ({
      getValue: () => values[key],
      getValueOrDefault: (defaultValue: number) => (values[key] === undefined ? defaultValue : (values[key] as number))
    })
  };
  return {
    getStateTree: () => ({
      getRootNode: () => ({ getMap: (feature: number) => (feature === RECONNECT_DIALOG_CONFIGURATION ? map : map) })
    })
  };
}

describe('ReconnectConfiguration', () => {
  afterEach(() => Reactive.flush());

  it('reads dialog texts and reconnect attempts/interval with defaults', () => {
    const config = new ReconnectConfiguration(makeRegistry({ dialogText: 'Reconnecting…', reconnectAttempts: 3 }));
    expect(config.getDialogText()).to.equal('Reconnecting…');
    expect(config.getDialogTextGaveUp()).to.equal(null);
    expect(config.getReconnectAttempts()).to.equal(3);
    expect(config.getReconnectInterval()).to.equal(5000); // default
  });

  it('bind runs the handler configurationUpdated on flush', () => {
    const calls: string[] = [];
    ReconnectConfiguration.bind({ configurationUpdated: () => calls.push('x') });
    Reactive.flush();
    expect(calls).to.deep.equal(['x']);
  });
});

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
