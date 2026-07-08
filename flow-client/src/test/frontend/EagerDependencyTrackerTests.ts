import { expect } from '@open-wc/testing';
import {
  endEagerDependencyLoading,
  resetForTesting,
  runWhenEagerDependenciesLoaded,
  startEagerDependencyLoading
} from '../../main/frontend/internal/EagerDependencyTracker';

describe('EagerDependencyTracker', () => {
  beforeEach(() => resetForTesting());

  it('runs the command immediately when no eager dependencies are loading', () => {
    let ran = false;
    runWhenEagerDependenciesLoaded(() => {
      ran = true;
    });
    expect(ran).to.be.true;
  });

  it('defers the command until all eager dependencies have loaded', () => {
    const order: string[] = [];
    startEagerDependencyLoading();
    runWhenEagerDependenciesLoaded(() => order.push('cmd'));
    expect(order).to.deep.equal([]); // still loading

    endEagerDependencyLoading();
    expect(order).to.deep.equal(['cmd']);
  });

  it('only runs the queued commands when the count returns to zero', () => {
    const order: string[] = [];
    startEagerDependencyLoading();
    startEagerDependencyLoading();
    runWhenEagerDependenciesLoaded(() => order.push('a'));
    runWhenEagerDependenciesLoaded(() => order.push('b'));

    endEagerDependencyLoading();
    expect(order).to.deep.equal([]); // one still loading
    endEagerDependencyLoading();
    expect(order).to.deep.equal(['a', 'b']);
  });

  it('runs commands queued by a command during the run (index loop)', () => {
    const order: string[] = [];
    startEagerDependencyLoading();
    runWhenEagerDependenciesLoaded(() => {
      order.push('first');
      // Count is 0 during the run, so this runs immediately rather than queueing.
      runWhenEagerDependenciesLoaded(() => order.push('nested-immediate'));
    });
    endEagerDependencyLoading();
    expect(order).to.deep.equal(['first', 'nested-immediate']);
  });
});
