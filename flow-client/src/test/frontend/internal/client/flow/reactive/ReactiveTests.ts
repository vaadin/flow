import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { TestReactiveEventRouter } from '../../../reactive/TestReactiveEventRouter';

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

  it('post flush listener is invoked during flush', () => {
    let count = 0;
    Reactive.addPostFlushListener(() => count++);
    expect(count).to.equal(0);
    Reactive.flush();
    expect(count).to.equal(1);
  });

  it('post flush listener is removed after flush', () => {
    let count = 0;
    Reactive.addPostFlushListener(() => count++);
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
