import { expect } from '@open-wc/testing';
import { ServerRpcQueue } from '../../main/frontend/internal/communication/ServerRpcQueue';

function makeRegistry(running = true) {
  let sends = 0;
  const registry = {
    getUILifecycle: () => ({ isRunning: () => running }),
    getMessageSender: () => ({
      sendInvocationsToServer: () => {
        sends++;
      }
    })
  };
  return { registry, sends: () => sends };
}

describe('ServerRpcQueue', () => {
  it('queues invocations while the UI is running', () => {
    const { registry } = makeRegistry(true);
    const queue = new ServerRpcQueue(registry);
    expect(queue.isEmpty()).to.be.true;
    queue.add({ a: 1 });
    queue.add({ b: 2 });
    expect(queue.size()).to.equal(2);
    expect(queue.toJson()).to.deep.equal([{ a: 1 }, { b: 2 }]);
  });

  it('ignores invocations when the UI is not running', () => {
    const { registry } = makeRegistry(false);
    const queue = new ServerRpcQueue(registry);
    queue.add({ a: 1 });
    expect(queue.isEmpty()).to.be.true;
  });

  it('flushes deferred, sending the invocations to the server', async () => {
    const { registry, sends } = makeRegistry(true);
    const queue = new ServerRpcQueue(registry);
    queue.add({ a: 1 });
    queue.flush();
    expect(queue.isFlushPending()).to.be.true;
    expect(sends()).to.equal(0); // deferred, not yet sent
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(sends()).to.equal(1);
  });

  it('flush is a no-op when the queue is empty', async () => {
    const { registry, sends } = makeRegistry(true);
    const queue = new ServerRpcQueue(registry);
    queue.flush();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(sends()).to.equal(0);
  });

  it('clear cancels a scheduled flush', async () => {
    const { registry, sends } = makeRegistry(true);
    const queue = new ServerRpcQueue(registry);
    queue.add({ a: 1 });
    queue.flush();
    queue.clear();
    expect(queue.isEmpty()).to.be.true;
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(sends()).to.equal(0); // cleared before the deferred ran
  });
});
