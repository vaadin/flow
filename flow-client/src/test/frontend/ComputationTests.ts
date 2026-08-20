import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/Reactive';
import { countingComputation } from './CountingComputation';
import { TestReactiveEventRouter } from './TestReactiveEventRouter';

describe('reactive core: Computation', () => {
  let router: TestReactiveEventRouter;
  beforeEach(() => {
    Reactive.reset();
    router = new TestReactiveEventRouter();
  });

  it('reruns only when dirty and flushed', () => {
    const { computation, getCount } = countingComputation(() => router.registerRead());
    expect(getCount()).to.equal(0);
    Reactive.flush();
    expect(getCount()).to.equal(1);
    Reactive.flush();
    expect(getCount()).to.equal(1);
    router.invalidate();
    expect(getCount()).to.equal(1);
    Reactive.flush();
    expect(getCount()).to.equal(2);
    void computation;
  });

  it('stopping before the initial flush prevents compute', () => {
    const { computation, getCount } = countingComputation(() => router.registerRead());
    computation.stop();
    Reactive.flush();
    expect(getCount()).to.equal(0);
  });

  it('stopping before invalidate prevents further compute', () => {
    const { computation, getCount } = countingComputation(() => router.registerRead());
    Reactive.flush();
    computation.stop();
    router.invalidate();
    Reactive.flush();
    expect(getCount()).to.equal(1);
  });

  it('stopping before reflush prevents recompute', () => {
    const { computation, getCount } = countingComputation(() => router.registerRead());
    Reactive.flush();
    router.invalidate();
    computation.stop();
    Reactive.flush();
    expect(getCount()).to.equal(1);
  });

  it('tracks changing dependencies', () => {
    const otherRouter = new TestReactiveEventRouter();
    let computeCount = 0;
    Reactive.runWhenDependenciesChange(() => {
      computeCount++;
      if (computeCount % 2 === 0) {
        router.registerRead();
      } else {
        otherRouter.registerRead();
      }
    });
    Reactive.flush();
    router.invalidate();
    Reactive.flush();
    expect(computeCount).to.equal(1);
    otherRouter.invalidate();
    Reactive.flush();
    expect(computeCount).to.equal(2);
    otherRouter.invalidate();
    Reactive.flush();
    expect(computeCount).to.equal(2);
    router.invalidate();
    Reactive.flush();
    expect(computeCount).to.equal(3);
  });

  it('reactive listeners fire once and are removed', () => {
    let computeCount = 0;
    Reactive.runWhenDependenciesChange(() => {
      if (computeCount === 0) {
        router.addListener(() => computeCount++);
        router.registerRead();
      }
    });
    Reactive.flush();
    expect(computeCount).to.equal(0);
    router.invalidate();
    expect(computeCount).to.equal(1);
    router.invalidate();
    expect(computeCount).to.equal(1);
    Reactive.flush();
    expect(computeCount).to.equal(1);
    router.invalidate();
    expect(computeCount).to.equal(1);
  });

  it('runWithComputation(null) escapes dependency tracking', () => {
    let computeCount = 0;
    Reactive.runWhenDependenciesChange(() => {
      computeCount++;
      Reactive.runWithComputation(null, () => router.registerRead());
    });
    Reactive.flush();
    expect(computeCount).to.equal(1);
    router.invalidate();
    Reactive.flush();
    expect(computeCount).to.equal(1);
  });

  it('fires invalidate events when stopping', () => {
    const { computation } = countingComputation(() => router.registerRead());
    let invalidateCount = 0;
    computation.onNextInvalidate(() => invalidateCount++);
    computation.stop();
    expect(invalidateCount).to.equal(1);
  });
});
