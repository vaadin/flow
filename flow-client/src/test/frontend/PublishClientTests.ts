import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { publishClient } from '../../main/frontend/internal/publishClient';
import type { ApplicationConfiguration, ApplicationConnection } from '../../main/frontend/internal/gwtExports';

const $wnd = window as any;

function fakeConnectionStubs() {
  return {
    isActive: sinon.stub().returns(true),
    getByNodeId: sinon.stub().returns(null),
    getNodeId: sinon.stub().returns(7),
    addDomBindingListener: sinon.stub(),
    poll: sinon.stub(),
    resolveUri: sinon.stub().returns('/resolved'),
    sendEventMessage: sinon.stub(),
    getUIId: sinon.stub().returns(3),
    connectWebComponent: sinon.stub(),
    debug: sinon.stub().returns({ debug: true }),
    getJavaClass: sinon.stub().returns('com.example.Foo'),
    isHiddenByServer: sinon.stub().returns(false),
    getElementStyleProperties: sinon.stub().returns({ color: 'red' }),
    getProfilingData: sinon.stub().returns([1, 2]),
    start: sinon.stub()
  };
}

function asConnection(stubs: ReturnType<typeof fakeConnectionStubs>): ApplicationConnection {
  return stubs as unknown as ApplicationConnection;
}

function fakeConfig(opts: { production?: boolean; requestTiming?: boolean } = {}): ApplicationConfiguration {
  return {
    getApplicationId: () => 'ROOT-1234567',
    getUIId: () => 0,
    isProductionMode: () => opts.production ?? true,
    isRequestTiming: () => opts.requestTiming ?? false,
    getServletVersion: () => '99.9',
    getExportedWebComponents: () => ['my-component']
  };
}

describe('publishClient', () => {
  beforeEach(() => {
    $wnd.Vaadin = { Flow: { clients: {} } };
  });

  it('publishes under the TestBench id (window-name suffix stripped)', () => {
    publishClient(asConnection(fakeConnectionStubs()), fakeConfig());
    expect($wnd.Vaadin.Flow.clients.ROOT).to.exist;
    expect($wnd.Vaadin.Flow.clients['ROOT-1234567']).to.be.undefined;
  });

  it('exposes the core API and delegates to the connection', () => {
    const stubs = fakeConnectionStubs();
    publishClient(asConnection(stubs), fakeConfig());
    const client = $wnd.Vaadin.Flow.clients.ROOT;

    expect(client.productionMode).to.be.true;
    expect(client.initializing).to.be.false;
    expect(client.exportedWebComponents).to.deep.equal(['my-component']);

    expect(client.isActive()).to.be.true;
    expect(client.getUIId()).to.equal(3);
    expect(client.resolveUri('foo')).to.equal('/resolved');
    expect(stubs.resolveUri.calledWith('foo')).to.be.true;

    client.sendEventMessage(2, 'click', null);
    expect(stubs.sendEventMessage.calledWith(2, 'click', null)).to.be.true;
  });

  it('omits dev-only and profiling methods in production without request timing', () => {
    publishClient(asConnection(fakeConnectionStubs()), fakeConfig());
    const client = $wnd.Vaadin.Flow.clients.ROOT;
    expect(client.getProfilingData).to.be.undefined;
    expect(client.debug).to.be.undefined;
    expect(client.getVersionInfo).to.be.undefined;
    expect(client.getNodeInfo).to.be.undefined;
  });

  it('adds getProfilingData when request timing is enabled', () => {
    const stubs = fakeConnectionStubs();
    publishClient(asConnection(stubs), fakeConfig({ requestTiming: true }));
    const client = $wnd.Vaadin.Flow.clients.ROOT;
    expect(client.getProfilingData()).to.deep.equal([1, 2]);
    expect(stubs.getProfilingData.called).to.be.true;
  });

  it('adds dev-mode methods when not in production', () => {
    const stubs = fakeConnectionStubs();
    publishClient(asConnection(stubs), fakeConfig({ production: false }));
    const client = $wnd.Vaadin.Flow.clients.ROOT;

    expect(client.productionMode).to.be.false;
    expect(client.getVersionInfo()).to.deep.equal({ flow: '99.9' });
    expect(client.debug()).to.deep.equal({ debug: true });
    expect(client.getNodeInfo(5)).to.deep.equal({
      element: null,
      javaClass: 'com.example.Foo',
      hiddenByServer: false,
      styles: { color: 'red' }
    });
    expect(stubs.getJavaClass.calledWith(5)).to.be.true;
  });
});
