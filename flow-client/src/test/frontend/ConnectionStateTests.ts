const {sinon} = intern.getPlugin('sinon');
const {describe, it} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');
const { assert } = intern.getPlugin("chai");

import {
  ConnectionState,
  ConnectionStateStore
} from "../../main/resources/META-INF/resources/frontend/ConnectionState";

describe('ConnectionStateStore', () => {

  it('should call state change listeners when transitioning between states', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener1 = sinon.fake();
    const stateChangeListener2 = sinon.fake();
    store.addStateChangeListener(stateChangeListener1);
    store.addStateChangeListener(stateChangeListener2);

    (expect(stateChangeListener1).to.not.be as any).called;
    (expect(stateChangeListener2).to.not.be as any).called;

    store.state = ConnectionState.CONNECTION_LOST;

    (expect(stateChangeListener1).to.be as any).calledOnce;
    (expect(stateChangeListener2).to.be as any).calledOnce;
  });

  it('should have removable state change listeners', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.fake();
    store.addStateChangeListener(stateChangeListener);

    store.removeStateChangeListener(stateChangeListener);
    store.state = ConnectionState.CONNECTION_LOST;
    (expect(stateChangeListener).to.not.be as any).called;
  });

  it('state change listeners should be idempotent with respect to state update', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.fake();
    store.addStateChangeListener(stateChangeListener);

    store.state = ConnectionState.CONNECTION_LOST;
    store.state = ConnectionState.CONNECTION_LOST;

    (expect(stateChangeListener).to.be as any).calledOnce;
  });

  it('state change listeners should be idempotent with respect to addStateChangeListener', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

    store.addStateChangeListener(stateChangeListener);
    store.addStateChangeListener(stateChangeListener);
    store.state = ConnectionState.CONNECTION_LOST;

    (expect(stateChangeListener).to.be as any).calledOnce;
  });

  it('LOADING states are stacked', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.fake();
    store.addStateChangeListener(stateChangeListener);

    store.loadingStarted();
    store.loadingStarted();
    store.loadingFinished();
    store.loadingFinished();

    assert.equal(stateChangeListener.callCount, 2);

    (expect(stateChangeListener.getCall(0)).to.be as any).calledWithExactly(
      ConnectionState.CONNECTED, ConnectionState.LOADING);
    (expect(stateChangeListener.getCall(1)).to.be as any).calledWithExactly(
      ConnectionState.LOADING, ConnectionState.CONNECTED);
  });

  it('loading count should reset when state forced', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.fake();
    store.addStateChangeListener(stateChangeListener);

    store.loadingStarted();
    store.state = ConnectionState.CONNECTION_LOST;
    store.loadingStarted();
    store.loadingFinished();

    assert.equal(stateChangeListener.callCount, 4);

    (expect(stateChangeListener.getCall(0)).to.be as any).calledWithExactly(
      ConnectionState.CONNECTED, ConnectionState.LOADING);
    (expect(stateChangeListener.getCall(1)).to.be as any).calledWithExactly(
      ConnectionState.LOADING, ConnectionState.CONNECTION_LOST);
    (expect(stateChangeListener.getCall(2)).to.be as any).calledWithExactly(
      ConnectionState.CONNECTION_LOST, ConnectionState.LOADING);
    (expect(stateChangeListener.getCall(3)).to.be as any).calledWithExactly(
      ConnectionState.LOADING, ConnectionState.CONNECTED);
  });

  it('loadingFailed should set state to CONNECTION_LOST', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTION_LOST);
    const stateChangeListener = sinon.fake();
    store.addStateChangeListener(stateChangeListener);

    store.loadingStarted();
    store.loadingFailed();

    assert.equal(stateChangeListener.callCount, 2);
    (expect(stateChangeListener.getCall(0)).to.be as any).calledWithExactly(
      ConnectionState.CONNECTION_LOST, ConnectionState.LOADING);
    (expect(stateChangeListener.getCall(1)).to.be as any).calledWithExactly(
      ConnectionState.LOADING, ConnectionState.CONNECTION_LOST);
  });
});
