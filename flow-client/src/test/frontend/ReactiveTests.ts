/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import { expect } from '@open-wc/testing';

import { Reactive } from '../../main/frontend/internal/client/flow/reactive/Reactive';

// Ported from src/test/java/com/vaadin/client/flow/reactive/ReactiveTest.java.
// Each `it` block mirrors one @Test method in the Java suite (test class
// was deleted in the same commit that migrated Reactive to TS).

describe('Reactive', () => {
  beforeEach(() => {
    Reactive.reset();
  });

  // testFlushListenersRemoved
  it('drops flush listeners after each flush', () => {
    let count = 0;
    Reactive.addFlushListener(() => count++);
    Reactive.addFlushListener(() => count++);

    expect(count).to.equal(0);

    Reactive.flush();
    expect(count).to.equal(2);

    Reactive.flush();
    expect(count).to.equal(2);
  });

  // addFlushListenerDuringFlush
  it('runs listeners added during a flush in the same flush', () => {
    let count = 0;
    Reactive.addFlushListener(() => {
      Reactive.addFlushListener(() => count++);
    });

    Reactive.flush();
    expect(count).to.equal(1);

    Reactive.flush();
    expect(count).to.equal(1);
  });

  // testCollectEvents — uses Reactive.notifyEventCollectors directly instead
  // of a ReactiveEventRouter (which is still Java; will move in a later
  // commit).
  it('fires events to collectors until they are removed', () => {
    let count = 0;
    const remove = Reactive.addEventCollector(() => count++);

    expect(count).to.equal(0);

    const event = {};
    Reactive.notifyEventCollectors(event);
    expect(count).to.equal(1);

    Reactive.notifyEventCollectors(event);
    expect(count).to.equal(2);

    remove();
    Reactive.notifyEventCollectors(event);
    expect(count).to.equal(2);
  });

  // testPostFlushListenerInvokedDuringFlush
  it('invokes post-flush listeners during flush', () => {
    let count = 0;
    Reactive.addPostFlushListener(() => count++);

    expect(count).to.equal(0);

    Reactive.flush();
    expect(count).to.equal(1);
  });

  // testPostFlushListenerRemovedAfterFlush
  it('drops post-flush listeners after they fire', () => {
    let count = 0;
    Reactive.addPostFlushListener(() => count++);

    Reactive.flush();
    expect(count).to.equal(1);

    Reactive.flush();
    expect(count).to.equal(1);
  });

  // testPostFlushListenerInvokedInAddOrder
  it('invokes post-flush listeners in registration order', () => {
    const order: number[] = [];
    for (let i = 0; i < 10; i++) {
      const captured = i;
      Reactive.addPostFlushListener(() => order.push(captured));
    }

    Reactive.flush();

    expect(order).to.deep.equal([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
  });

  // testPostFlushListenerInvokedAfterRegularFlushListener
  it('drains flush listeners before any post-flush listener', () => {
    const order: string[] = [];
    Reactive.addPostFlushListener(() => order.push('postFlush'));
    Reactive.addFlushListener(() => order.push('flush'));

    expect(order).to.deep.equal([]);

    Reactive.flush();
    expect(order).to.deep.equal(['flush', 'postFlush']);
  });

  // testNewFlushListenerInvokedBeforeNextPostListener
  it('runs a flush listener added by a post-flush listener before the next post-flush listener', () => {
    const order: string[] = [];
    Reactive.addPostFlushListener(() => order.push('postFlush1'));
    Reactive.addPostFlushListener(() => Reactive.addFlushListener(() => order.push('flush2')));
    Reactive.addPostFlushListener(() => order.push('postFlush2'));
    Reactive.addFlushListener(() => order.push('flush1'));

    Reactive.flush();

    expect(order).to.deep.equal(['flush1', 'postFlush1', 'flush2', 'postFlush2']);
  });

  // flushRunning_newFlushIsIgnored
  it('re-entrant flush() is a no-op', () => {
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
