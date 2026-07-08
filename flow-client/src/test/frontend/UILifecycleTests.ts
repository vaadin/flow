import { expect } from '@open-wc/testing';
import { UILifecycle, UIState } from '../../main/frontend/internal/UILifecycle';

describe('UILifecycle', () => {
  it('starts in INITIALIZING and is neither running nor terminated', () => {
    const lifecycle = new UILifecycle();
    expect(lifecycle.getState()).to.equal(UIState.INITIALIZING);
    expect(lifecycle.isRunning()).to.be.false;
    expect(lifecycle.isTerminated()).to.be.false;
  });

  it('advances INITIALIZING -> RUNNING -> TERMINATED, firing events', () => {
    const lifecycle = new UILifecycle();
    const seen: string[] = [];
    lifecycle.addHandler((event) => {
      expect(event.getUiLifecycle()).to.equal(lifecycle);
      seen.push(lifecycle.getState());
    });

    lifecycle.setState(UIState.RUNNING);
    expect(lifecycle.isRunning()).to.be.true;
    lifecycle.setState(UIState.TERMINATED);
    expect(lifecycle.isTerminated()).to.be.true;
    expect(seen).to.deep.equal([UIState.RUNNING, UIState.TERMINATED]);
  });

  it('rejects skipping, reversing, or repeating a state', () => {
    expect(() => new UILifecycle().setState(UIState.TERMINATED)).to.throw('not allowed'); // skip RUNNING
    expect(() => new UILifecycle().setState(UIState.INITIALIZING)).to.throw('not allowed'); // same state

    const running = new UILifecycle();
    running.setState(UIState.RUNNING);
    expect(() => running.setState(UIState.INITIALIZING)).to.throw('not allowed'); // reverse
    expect(() => running.setState(UIState.RUNNING)).to.throw('not allowed'); // repeat
  });

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
