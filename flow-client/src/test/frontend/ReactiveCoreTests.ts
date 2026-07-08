import { expect } from '@open-wc/testing';
import {
  Computation,
  Reactive,
  ReactiveEventRouter,
  ReactiveValueChangeEvent,
  type ReactiveValue,
  type ReactiveValueChangeListener
} from '../../main/frontend/internal/reactive/reactive';

// Mirrors the Java TestReactiveEventRouter test helper: a router whose source
// routes change registrations back to itself, with an invalidate() that fires a
// change event.
class TestReactiveEventRouter extends ReactiveEventRouter<ReactiveValueChangeListener, ReactiveValueChangeEvent> {
  constructor() {
    const source: ReactiveValue = {
      addReactiveValueChangeListener: () => {
        throw new Error('event source not wired yet');
      }
    };
    super(
      source,
      (l) => l,
      (l, e) => l(e)
    );
    source.addReactiveValueChangeListener = (l) => this.addReactiveListener(l);
  }

  invalidate(): void {
    this.fireEvent(new ReactiveValueChangeEvent(this.getReactiveValue()));
  }
}

// Mirrors the Java CountingComputation helper: counts recomputations and runs a
// reader on each.
function countingComputation(reader: () => void): { computation: Computation; getCount: () => number } {
  let count = 0;
  const computation = new Computation(() => {
    count++;
    reader();
  });
  return { computation, getCount: () => count };
}

describe('reactive core: Reactive', () => {
  beforeEach(() => Reactive.reset());

  it('flush listeners are removed after each flush', () => {
    let count = 0;
    Reactive.addFlushListener(() => count++);
    Reactive.addFlushListener(() => count++);
    expect(count).to.equal(0);
    Reactive.flush();
    expect(count).to.equal(2);
    Reactive.flush();
    expect(count).to.equal(2);
  });

  it('a listener added during flush runs in the same flush, once', () => {
    let count = 0;
    Reactive.addFlushListener(() => Reactive.addFlushListener(() => count++));
    Reactive.flush();
    expect(count).to.equal(1);
    Reactive.flush();
    expect(count).to.equal(1);
  });

  it('event collectors receive events until removed', () => {
    const router = new TestReactiveEventRouter();
    let count = 0;
    const remover = Reactive.addEventCollector(() => count++);
    expect(count).to.equal(0);
    router.invalidate();
    expect(count).to.equal(1);
    router.invalidate();
    expect(count).to.equal(2);
    remover.remove();
    router.invalidate();
    expect(count).to.equal(2);
  });

  it('post flush listener is invoked during flush and removed after', () => {
    let count = 0;
    Reactive.addPostFlushListener(() => count++);
    expect(count).to.equal(0);
    Reactive.flush();
    expect(count).to.equal(1);
    Reactive.flush();
    expect(count).to.equal(1);
  });

  it('post flush listeners run in add order', () => {
    const order: number[] = [];
    for (let i = 0; i < 10; i++) {
      const captured = i;
      Reactive.addPostFlushListener(() => order.push(captured));
    }
    Reactive.flush();
    expect(order).to.deep.equal([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
  });

  it('post flush listeners run after regular flush listeners', () => {
    const order: string[] = [];
    Reactive.addPostFlushListener(() => order.push('postFlush'));
    Reactive.addFlushListener(() => order.push('flush'));
    expect(order).to.deep.equal([]);
    Reactive.flush();
    expect(order).to.deep.equal(['flush', 'postFlush']);
  });

  it('a new flush listener runs before the next post flush listener', () => {
    const order: string[] = [];
    Reactive.addPostFlushListener(() => order.push('postFlush1'));
    Reactive.addPostFlushListener(() => Reactive.addFlushListener(() => order.push('flush2')));
    Reactive.addPostFlushListener(() => order.push('postFlush2'));
    Reactive.addFlushListener(() => order.push('flush1'));
    Reactive.flush();
    expect(order).to.deep.equal(['flush1', 'postFlush1', 'flush2', 'postFlush2']);
  });

  it('a flush started while flushing is ignored', () => {
    const order: string[] = [];
    Reactive.addPostFlushListener(() => order.push('postFlush'));
    Reactive.addFlushListener(() => order.push('flush'));
    Reactive.addFlushListener(() => {
      Reactive.flush();
      order.push('flush2');
    });
    expect(order).to.deep.equal([]);
    Reactive.flush();
    expect(order).to.deep.equal(['flush', 'flush2', 'postFlush']);
  });
});

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
