import { expect } from '@open-wc/testing';
import { ReconnectConfiguration } from '../../../../../main/frontend/internal/client/communication/ReconnectConfiguration';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { Reactive } from '../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { inertRegistry } from '../flow/stateTreeTestRegistry';
import { fakeConnectionStateHandler } from './connectionStateHandlerFake';

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
  const tree = new StateTree(inertRegistry());
  const map = tree.getRootNode().getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION);
  return {
    getProperty: (key: string) => map.getProperty(key),
    getStateTree: () => tree
  };
}

describe('ReconnectConfiguration', () => {
  let registry: ReturnType<typeof makeRegistry>;
  let config: ReconnectConfiguration;
  let configurationUpdatedCalled: number;

  beforeEach(() => {
    registry = makeRegistry();
    config = new ReconnectConfiguration(registry);
    configurationUpdatedCalled = 0;
    ReconnectConfiguration.bind(
      fakeConnectionStateHandler({
        configurationUpdated: () => {
          // Reads a value so the reactive computation tracks the configuration.
          config.getDialogText();
          configurationUpdatedCalled += 1;
        }
      })
    );
    // No flush here: Java's setup binds without flushing, so the first flush in
    // a test also performs the computation's initial run.
    configurationUpdatedCalled = 0;
  });

  afterEach(() => Reactive.flush());

  it('defaults to null dialog texts and default attempts/interval', () => {
    expect(config.getDialogText()).to.equal(null);
    expect(config.getDialogTextGaveUp()).to.equal(null);
    expect(config.getReconnectAttempts()).to.equal(RECONNECT_ATTEMPTS_DEFAULT);
    expect(config.getReconnectInterval()).to.equal(RECONNECT_INTERVAL_DEFAULT);
  });

  it('sets and gets the dialog text', () => {
    registry.getProperty(DIALOG_TEXT_KEY).setValue('foo');
    expect(config.getDialogText()).to.equal('foo');
    registry.getProperty(DIALOG_TEXT_KEY).setValue('bar');
    expect(config.getDialogText()).to.equal('bar');
  });

  it('sets and gets the gave-up dialog text', () => {
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('foo');
    expect(config.getDialogTextGaveUp()).to.equal('foo');
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('bar');
    expect(config.getDialogTextGaveUp()).to.equal('bar');
  });

  it('sets and gets the reconnect attempts', () => {
    // Numbers are always passed as doubles from the server.
    registry.getProperty(RECONNECT_ATTEMPTS_KEY).setValue(1234.0);
    expect(config.getReconnectAttempts()).to.equal(1234);
    registry.getProperty(RECONNECT_ATTEMPTS_KEY).setValue(1.0);
    expect(config.getReconnectAttempts()).to.equal(1);
  });

  it('sets and gets the reconnect interval', () => {
    // Numbers are always passed as doubles from the server.
    registry.getProperty(RECONNECT_INTERVAL_KEY).setValue(1234.0);
    expect(config.getReconnectInterval()).to.equal(1234);
    registry.getProperty(RECONNECT_INTERVAL_KEY).setValue(1.0);
    expect(config.getReconnectInterval()).to.equal(1);
  });

  it('reacts to changes, reporting each flushed change once', () => {
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('bar');
    Reactive.flush();
    expect(configurationUpdatedCalled).to.equal(1);

    registry.getProperty(DIALOG_TEXT_KEY).setValue('foo');
    Reactive.flush();
    expect(configurationUpdatedCalled).to.equal(2);
  });

  it('reports several changes made before a flush in one batch', () => {
    registry.getProperty(RECONNECT_INTERVAL_KEY).setValue(13.0);
    registry.getProperty(RECONNECT_ATTEMPTS_KEY).setValue(13.0);
    registry.getProperty(DIALOG_TEXT_KEY).setValue('abc');
    registry.getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue('def');
    expect(configurationUpdatedCalled).to.equal(0);
    Reactive.flush();
    expect(configurationUpdatedCalled).to.equal(1);
  });

  describe('beyond the Java suite', () => {
    it('re-runs on a change to a property the handler read', () => {
      // Both Java cases assert their first count right after binding, so it is
      // the computation's initial run that satisfies them. Flushing that run
      // first makes the next count prove the dependency was registered.
      Reactive.flush();
      configurationUpdatedCalled = 0;

      registry.getProperty(DIALOG_TEXT_KEY).setValue('foo');
      Reactive.flush();
      expect(configurationUpdatedCalled).to.equal(1);

      registry.getProperty(DIALOG_TEXT_KEY).setValue('bar');
      Reactive.flush();
      expect(configurationUpdatedCalled).to.equal(2);
    });
  });
});
