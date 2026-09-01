// Every case here is beyond the Java suite: com.vaadin.client.DefaultRegistry has
// no Java test class. RegistryTest covers the base Registry it extends, and is
// ported in RegistryTests.

import { expect } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../../../main/frontend/internal/client/ApplicationConfiguration';
import { ApplicationConnection } from '../../../../main/frontend/internal/client/ApplicationConnection';
import { DefaultConnectionStateHandler } from '../../../../main/frontend/internal/client/communication/DefaultConnectionStateHandler';
import { DefaultRegistry } from '../../../../main/frontend/internal/client/DefaultRegistry';
import { MessageSender } from '../../../../main/frontend/internal/client/communication/MessageSender';
import { UILifecycle } from '../../../../main/frontend/internal/client/UILifecycle';

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

  it('hands out the application connection once it has been set', () => {
    // Java passes the connection into the constructor. The port cannot, because
    // it builds the registry before the connection, so ApplicationConnection.create
    // registers it as soon as it exists - its only caller. Until then the lookup
    // fails rather than returning undefined.
    const registry = makeRegistry();
    expect(() => registry.getApplicationConnection()).to.throw();

    const connection = new ApplicationConnection(registry, { hasWorkQueued: () => false });
    registry.setApplicationConnection(connection);
    expect(registry.getApplicationConnection()).to.equal(connection);
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
