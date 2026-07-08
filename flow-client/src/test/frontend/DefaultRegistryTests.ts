import { expect } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../main/frontend/internal/ApplicationConfiguration';
import { DefaultConnectionStateHandler } from '../../main/frontend/internal/communication/DefaultConnectionStateHandler';
import { DefaultRegistry } from '../../main/frontend/internal/DefaultRegistry';
import { MessageSender } from '../../main/frontend/internal/communication/MessageSender';
import { UILifecycle } from '../../main/frontend/internal/UILifecycle';

function makeRegistry(): DefaultRegistry {
  const config = new ApplicationConfiguration();
  config.setServiceUrl('/app');
  config.setUIId(1);
  config.setHeartbeatInterval(-1); // keep the heartbeat timer disabled in tests
  return new DefaultRegistry(config);
}

describe('DefaultRegistry', () => {
  it('constructs and wires all services', () => {
    const registry = makeRegistry();
    expect(registry.getApplicationConfiguration().getServiceUrl()).to.equal('/app');
    expect(registry.getMessageSender()).to.be.instanceOf(MessageSender);
    expect(registry.getConnectionStateHandler()).to.be.instanceOf(DefaultConnectionStateHandler);
    expect(registry.getUILifecycle()).to.be.instanceOf(UILifecycle);
    // The state tree, server connector, message handler, etc. are all present.
    expect(registry.getStateTree().getRootNode()).to.not.equal(null);
    expect(registry.getMessageHandler().getCsrfToken()).to.equal('init');
  });

  it('cross-wires services: XhrConnection.getUri() resolves the configuration', () => {
    // XhrConnection reads registry.getApplicationConfiguration() for its URI,
    // proving the registry hands each service its collaborators.
    expect(makeRegistry().getXhrConnection().getUri()).to.equal('/app?v-r=uidl&v-uiId=1');
  });

  it('recreates resettable singletons on reset (UILifecycle), keeping finals', () => {
    const registry = makeRegistry();
    const config = registry.getApplicationConfiguration();
    const lifecycleBefore = registry.getUILifecycle();

    registry.reset();
    expect(registry.getUILifecycle()).to.not.equal(lifecycleBefore); // resettable -> new instance
    expect(registry.getApplicationConfiguration()).to.equal(config); // final -> same instance
  });
});
