import { expect } from '@open-wc/testing';
import { ReconnectConfiguration } from '../../main/frontend/internal/communication/ReconnectConfiguration';
import { XhrConnectionError } from '../../main/frontend/internal/communication/XhrConnectionError';
import { MapProperty, type MapPropertyOwner } from '../../main/frontend/internal/nodefeature/MapProperty';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';

const RECONNECT_DIALOG_CONFIGURATION = 9;

// Mirrors ReconnectDialogConfigurationMap.
const DIALOG_TEXT_KEY = 'dialogText';
const DIALOG_TEXT_GAVE_UP_KEY = 'dialogTextGaveUp';
const RECONNECT_ATTEMPTS_KEY = 'reconnectAttempts';
const RECONNECT_ATTEMPTS_DEFAULT = 10000;
const RECONNECT_INTERVAL_KEY = 'reconnectInterval';
const RECONNECT_INTERVAL_DEFAULT = 5000;

// A registry backed by real MapProperty instances so that reads register
// reactive dependencies and setValue fires reactive changes, exactly like the
// Java test that binds ReconnectConfiguration to a StateTree root node.
function makeRegistry() {
  const properties: Record<string, MapProperty> = {};
  // setValue never touches the tree, so a minimal owner is enough here.
  const owner: MapPropertyOwner = {
    getNode: () => ({ getTree: () => ({ isActive: () => true, sendNodePropertySyncToServer: () => {} }) })
  };
  const map = {
    getProperty: (key: string) => {
      properties[key] ??= new MapProperty(key, owner);
      return properties[key];
    }
  };
  return {
    getProperty: (key: string) => map.getProperty(key),
    getStateTree: () => ({
      getRootNode: () => ({
        getMap: (feature: number) => {
          expect(feature).to.equal(RECONNECT_DIALOG_CONFIGURATION);
          return map;
        }
      })
    })
  };
}

describe('ReconnectConfiguration', () => {
  afterEach(() => Reactive.flush());

  it('defaults to null dialog texts and default attempts/interval', () => {
    const config = new ReconnectConfiguration(makeRegistry());
    expect(config.getDialogText()).to.equal(null);
    expect(config.getDialogTextGaveUp()).to.equal(null);
    expect(config.getReconnectAttempts()).to.equal(RECONNECT_ATTEMPTS_DEFAULT);
    expect(config.getReconnectInterval()).to.equal(RECONNECT_INTERVAL_DEFAULT);
  });

  it('sets and gets the dialog text', () => {
    const registry = makeRegistry();
    const config = new ReconnectConfiguration(registry);
    registry.getProperty(DIALOG_TEXT_KEY).setValue('foo');
    expect(config.getDialogText()).to.equal('foo');
    registry.getProperty(DIALOG_TEXT_KEY).setValue('bar');
    expect(config.getDialogText()).to.equal('bar');
  });

  it('sets and gets the gave-up dialog text', () => {
    const registry = makeRegistry();
    const config = new ReconnectConfiguration(registry);
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('foo');
    expect(config.getDialogTextGaveUp()).to.equal('foo');
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('bar');
    expect(config.getDialogTextGaveUp()).to.equal('bar');
  });

  it('sets and gets the reconnect attempts', () => {
    const registry = makeRegistry();
    const config = new ReconnectConfiguration(registry);
    // Numbers are always passed as doubles from the server.
    registry.getProperty(RECONNECT_ATTEMPTS_KEY).setValue(1234.0);
    expect(config.getReconnectAttempts()).to.equal(1234);
    registry.getProperty(RECONNECT_ATTEMPTS_KEY).setValue(1.0);
    expect(config.getReconnectAttempts()).to.equal(1);
  });

  it('sets and gets the reconnect interval', () => {
    const registry = makeRegistry();
    const config = new ReconnectConfiguration(registry);
    // Numbers are always passed as doubles from the server.
    registry.getProperty(RECONNECT_INTERVAL_KEY).setValue(1234.0);
    expect(config.getReconnectInterval()).to.equal(1234);
    registry.getProperty(RECONNECT_INTERVAL_KEY).setValue(1.0);
    expect(config.getReconnectInterval()).to.equal(1);
  });

  it('reacts to changes, reporting each flushed change once', () => {
    const registry = makeRegistry();
    const config = new ReconnectConfiguration(registry);
    let configurationUpdatedCalled = 0;
    // Reads a value like the Java handler so the reactive computation tracks it.
    ReconnectConfiguration.bind({
      configurationUpdated: () => {
        config.getDialogText();
        configurationUpdatedCalled += 1;
      }
    });

    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('bar');
    Reactive.flush();
    expect(configurationUpdatedCalled).to.equal(1);

    registry.getProperty(DIALOG_TEXT_KEY).setValue('foo');
    Reactive.flush();
    expect(configurationUpdatedCalled).to.equal(2);
  });

  it('reports several changes made before a flush in one batch', () => {
    const registry = makeRegistry();
    const config = new ReconnectConfiguration(registry);
    let configurationUpdatedCalled = 0;
    ReconnectConfiguration.bind({
      configurationUpdated: () => {
        config.getDialogText();
        configurationUpdatedCalled += 1;
      }
    });

    registry.getProperty(RECONNECT_INTERVAL_KEY).setValue(13.0);
    registry.getProperty(RECONNECT_ATTEMPTS_KEY).setValue(13.0);
    registry.getProperty(DIALOG_TEXT_KEY).setValue('abc');
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('def');
    expect(configurationUpdatedCalled).to.equal(0);
    Reactive.flush();
    expect(configurationUpdatedCalled).to.equal(1);
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
