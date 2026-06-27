import { expect } from '@open-wc/testing';
import {
  getServerId,
  isResynchronize,
  PendingMessageQueue
} from '../../main/frontend/internal/communication/MessageOrdering';

describe('MessageOrdering', () => {
  it('reads the server id and resync flag from a message', () => {
    expect(getServerId({ syncId: 7 })).to.equal(7);
    expect(getServerId({})).to.equal(-1);
    expect(isResynchronize({ resynchronize: true })).to.be.true;
    expect(isResynchronize({})).to.be.false;
  });

  it('treats any message as expected before the first response', () => {
    const queue = new PendingMessageQueue();
    expect(queue.getLastSeenServerSyncId()).to.equal(-1);
    expect(queue.getExpectedServerId()).to.equal(0);
    expect(queue.isNextExpectedMessage(5)).to.be.true; // first message always ok
    expect(queue.isNextExpectedMessage(-1)).to.be.true;
  });

  it('expects strictly the next id once a response has been seen', () => {
    const queue = new PendingMessageQueue();
    queue.setLastSeenServerSyncId(5);
    expect(queue.getExpectedServerId()).to.equal(6);
    expect(queue.isNextExpectedMessage(6)).to.be.true;
    expect(queue.isNextExpectedMessage(7)).to.be.false;
    expect(queue.isNextExpectedMessage(-1)).to.be.true;
    expect(queue.isAlreadySeen(5)).to.be.true;
    expect(queue.isAlreadySeen(6)).to.be.false;
  });

  it('finds the next handlable pending message by server id', () => {
    const queue = new PendingMessageQueue();
    queue.setLastSeenServerSyncId(5); // expecting 6
    queue.push({ syncId: 8 });
    queue.push({ syncId: 6 });
    expect(queue.findNextHandlable()).to.equal(1); // the id-6 message
    const handled = queue.remove(1);
    expect(handled.syncId).to.equal(6);
    expect(queue.length()).to.equal(1);
  });

  it('drops pending messages older than the expected id', () => {
    const queue = new PendingMessageQueue();
    queue.setLastSeenServerSyncId(5); // expecting 6
    queue.push({ syncId: 4 }); // old -> dropped
    queue.push({ syncId: 6 }); // current -> kept
    queue.push({}); // no id (-1) -> kept
    queue.removeOld();
    expect(queue.length()).to.equal(2);
    expect(queue.findNextHandlable()).to.equal(0); // id-6 is now first
  });

  it('supports clearing the queue', () => {
    const queue = new PendingMessageQueue();
    queue.push({ syncId: 1 });
    expect(queue.isEmpty()).to.be.false;
    queue.clear();
    expect(queue.isEmpty()).to.be.true;
  });
});
