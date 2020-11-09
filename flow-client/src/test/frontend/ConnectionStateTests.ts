const {sinon} = intern.getPlugin('sinon');
const {describe, it} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');

import {
  ConnectionState,
  ConnectionStateStore
} from "../../main/resources/META-INF/resources/frontend/ConnectionState";

describe('ConnectionStateStore', () => {

  it('should call state change listeners when transitioning between states', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener1 = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });
    const stateChangeListener2 = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

    store.addStateChangeListener(stateChangeListener1);
    store.addStateChangeListener(stateChangeListener2);
    (expect(stateChangeListener1).to.not.be as any).called;
    (expect(stateChangeListener2).to.not.be as any).called;

    store.setState(ConnectionState.CONNECTION_LOST);
    (expect(stateChangeListener1).to.be as any).calledOnce;
    (expect(stateChangeListener2).to.be as any).calledOnce;
  });

  it('should have removable state change listeners', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

    store.addStateChangeListener(stateChangeListener);
    store.removeStateChangeListener(stateChangeListener);
    store.setState(ConnectionState.CONNECTION_LOST);
    (expect(stateChangeListener).to.not.be as any).called;
  });

  it('state change listeners should be idempotent with respect to setState', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

    store.addStateChangeListener(stateChangeListener);
    store.setState(ConnectionState.CONNECTION_LOST);
    store.setState(ConnectionState.CONNECTION_LOST);

    (expect(stateChangeListener).to.be as any).calledOnce;
  });

  it('state change listeners should be idempotent with respect to addStateChangeListener', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

    store.addStateChangeListener(stateChangeListener);
    store.addStateChangeListener(stateChangeListener);
    store.setState(ConnectionState.CONNECTION_LOST);

    (expect(stateChangeListener).to.be as any).calledOnce;
  });
});
