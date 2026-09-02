import { expect } from '@open-wc/testing';
import { UILifecycle, UIState } from '../../../../main/frontend/internal/client/UILifecycle';

// Ported from com.vaadin.client.UILifecycleTest.
describe('UILifecycle', () => {
  it('starts in INITIALIZING', () => {
    // Ported from initialState.
    const lifecycle = new UILifecycle();
    expect(lifecycle.getState()).to.equal(UIState.INITIALIZING);
    expect(lifecycle.isRunning()).to.be.false;
    expect(lifecycle.isTerminated()).to.be.false;
  });

  it('advances INITIALIZING -> RUNNING -> TERMINATED', () => {
    // Ported from initialToRunningToTerminated.
    const lifecycle = new UILifecycle();

    lifecycle.setState(UIState.RUNNING);
    expect(lifecycle.getState()).to.equal(UIState.RUNNING);
    expect(lifecycle.isRunning()).to.be.true;

    lifecycle.setState(UIState.TERMINATED);
    expect(lifecycle.getState()).to.equal(UIState.TERMINATED);
    expect(lifecycle.isTerminated()).to.be.true;
  });

  it('rejects INITIALIZING -> TERMINATED', () => {
    // Ported from invalidStateChangeInitToTerminated.
    expect(() => new UILifecycle().setState(UIState.TERMINATED)).to.throw('not allowed');
  });

  it('rejects RUNNING -> INITIALIZING', () => {
    // Ported from invalidStateChangeRunningToInit.
    const lifecycle = new UILifecycle();
    lifecycle.setState(UIState.RUNNING);
    expect(() => lifecycle.setState(UIState.INITIALIZING)).to.throw('not allowed');
  });

  it('rejects TERMINATED -> INITIALIZING', () => {
    // Ported from invalidStateChangeTerminatedToInit.
    const lifecycle = new UILifecycle();
    lifecycle.setState(UIState.RUNNING);
    lifecycle.setState(UIState.TERMINATED);
    expect(() => lifecycle.setState(UIState.INITIALIZING)).to.throw('not allowed');
  });

  it('rejects TERMINATED -> RUNNING', () => {
    // Ported from invalidStateChangeTerminatedToRunning.
    const lifecycle = new UILifecycle();
    lifecycle.setState(UIState.RUNNING);
    lifecycle.setState(UIState.TERMINATED);
    expect(() => lifecycle.setState(UIState.RUNNING)).to.throw('not allowed');
  });

  it('fires a state change event per transition', () => {
    // Ported from stateChangeEvents.
    const lifecycle = new UILifecycle();
    let events = 0;
    lifecycle.addHandler((event) => {
      expect(event.getUiLifecycle()).to.equal(lifecycle);
      events += 1;
    });

    expect(events).to.equal(0);
    lifecycle.setState(UIState.RUNNING);
    expect(events).to.equal(1);
    lifecycle.setState(UIState.TERMINATED);
    expect(events).to.equal(2);
  });

  // Beyond the Java suite: the Java handler registration is removed through
  // GWT's HandlerRegistration, which the port replaces with an EventRemover.
  describe('beyond the Java suite', () => {
    it('detaches a handler via its remover', () => {
      const lifecycle = new UILifecycle();
      let count = 0;
      const remover = lifecycle.addHandler(() => count++);
      lifecycle.setState(UIState.RUNNING);
      remover.remove();
      lifecycle.setState(UIState.TERMINATED);
      expect(count).to.equal(1);
    });
  });
});
