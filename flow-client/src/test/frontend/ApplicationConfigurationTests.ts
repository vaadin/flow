import { expect } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../main/frontend/internal/ApplicationConfiguration';

describe('ApplicationConfiguration', () => {
  it('round-trips the URL / id / timeout configuration', () => {
    const config = new ApplicationConfiguration();
    config.setServiceUrl('/app?v-r=uidl');
    config.setContextRootUrl('/ctx/');
    config.setUIId(7);
    config.setHeartbeatInterval(300);
    config.setMaxMessageSuspendTimeout(5000);

    expect(config.getServiceUrl()).to.equal('/app?v-r=uidl');
    expect(config.getContextRootUrl()).to.equal('/ctx/');
    expect(config.getUIId()).to.equal(7);
    expect(config.getHeartbeatInterval()).to.equal(300);
    expect(config.getMaxMessageSuspendTimeout()).to.equal(5000);
  });

  it('round-trips the boolean modes with sensible defaults', () => {
    const config = new ApplicationConfiguration();
    expect(config.isProductionMode()).to.be.false;
    expect(config.isWebComponentMode()).to.be.false;
    expect(config.isDevToolsEnabled()).to.be.false;

    config.setProductionMode(true);
    config.setWebComponentMode(true);
    config.setDevToolsEnabled(true);
    expect(config.isProductionMode()).to.be.true;
    expect(config.isWebComponentMode()).to.be.true;
    expect(config.isDevToolsEnabled()).to.be.true;
  });

  it('round-trips exported web components and version strings', () => {
    const config = new ApplicationConfiguration();
    expect(config.getExportedWebComponents()).to.deep.equal([]);
    config.setExportedWebComponents(['my-button', 'my-field']);
    expect(config.getExportedWebComponents()).to.deep.equal(['my-button', 'my-field']);

    config.setAtmosphereVersion('3.1.0');
    expect(config.getAtmosphereVersion()).to.equal('3.1.0');
  });
});
