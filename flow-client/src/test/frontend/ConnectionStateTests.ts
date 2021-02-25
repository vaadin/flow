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
    const stateChangeListener1 = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });
    const stateChangeListener2 = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

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
    const stateChangeListener = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

    store.addStateChangeListener(stateChangeListener);
    store.removeStateChangeListener(stateChangeListener);
    store.state = ConnectionState.CONNECTION_LOST;
    (expect(stateChangeListener).to.not.be as any).called;
  });

  it('state change listeners should be idempotent with respect to state update', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const stateChangeListener = sinon.spy((_: ConnectionState, __: ConnectionState) => {
    });

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
    const states = Array<[ConnectionState,ConnectionState]>();
    const stateChangeListener = (prev: ConnectionState, next: ConnectionState) => {
      states.push([prev, next]);
    };
    store.addStateChangeListener(stateChangeListener);

    store.loadingStarted();
    store.loadingStarted();
    store.loadingSucceeded();
    store.loadingSucceeded();
    assert.deepEqual(states,
      [[ConnectionState.CONNECTED, ConnectionState.LOADING],
        [ConnectionState.LOADING, ConnectionState.CONNECTED]]);
  });

  it('loading count should reset when state forced', () => {
    const store = new ConnectionStateStore(ConnectionState.CONNECTED);
    const states = Array<[ConnectionState,ConnectionState]>();
    const stateChangeListener = (prev: ConnectionState, next: ConnectionState) => {
      states.push([prev, next]);
    };
    store.addStateChangeListener(stateChangeListener);

    store.loadingStarted();
    store.state = ConnectionState.CONNECTION_LOST;
    store.loadingStarted();
    store.loadingSucceeded();

    assert.deepEqual(states,
      [[ConnectionState.CONNECTED, ConnectionState.LOADING],
        [ConnectionState.LOADING, ConnectionState.CONNECTION_LOST],
        [ConnectionState.CONNECTION_LOST, ConnectionState.LOADING],
        [ConnectionState.LOADING, ConnectionState.CONNECTED]]);
  });

  it('should request offline information from from service worker', async() => {
    const $wnd = (window as any);

    const fakeServiceWorker = new EventTarget();
    sinon.spy(fakeServiceWorker, 'addEventListener');
    sinon.spy(fakeServiceWorker, 'removeEventListener');

    const navigatorStub = sinon.stub($wnd, 'navigator')
        .get(() => ({serviceWorker: fakeServiceWorker}));

    try {
      const postMessage = sinon.spy();
      const fakePromise = Promise.resolve({active: {postMessage}});
      Object.defineProperty(fakeServiceWorker, 'ready', {get: () => fakePromise});

      const store = new ConnectionStateStore(ConnectionState.CONNECTED);
      const listener = (store as any).serviceWorkerMessageListener;
      // should add message event listener on service worker
      sinon.assert.calledOnce(fakeServiceWorker.addEventListener)
      sinon.assert.calledWith(fakeServiceWorker.addEventListener, 'message', listener);

      // should send {type: "isConnectionLost"} to service worker
      await fakePromise;
      sinon.assert.calledOnce(postMessage);
      sinon.assert.calledWith(postMessage, {
        method: 'Vaadin.ServiceWorker.isConnectionLost',
        id: 'Vaadin.ServiceWorker.isConnectionLost'
      });

      // should transition to CONNECTION_LOST when receiving {result: true}
      const messageEvent = new MessageEvent('message', {
        data: {
          id: 'Vaadin.ServiceWorker.isConnectionLost',
          result: true
        }
      }) as any;
      fakeServiceWorker.dispatchEvent(messageEvent);
      assert.equal(store.state, ConnectionState.CONNECTION_LOST);

      // should remove message event listener on service worker
      sinon.assert.calledOnce(fakeServiceWorker.removeEventListener);
      sinon.assert.calledWith(fakeServiceWorker.removeEventListener, 'message', listener);
    } finally {
      navigatorStub.restore();
    }
  });
});
