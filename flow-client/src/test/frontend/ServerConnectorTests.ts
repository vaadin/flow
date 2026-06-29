import { expect } from '@open-wc/testing';
import { ServerConnector } from '../../main/frontend/internal/communication/ServerConnector';

// Captures enqueued messages and processMessage calls.
function makeRegistry() {
  const queued: Array<Record<string, unknown>> = [];
  const processed: Array<[string | null, string | null]> = [];
  let flushes = 0;
  const registry = {
    getLoadingIndicatorStateHandler: () => ({
      processMessage: (type: string | null, eventType: string | null) => processed.push([type, eventType])
    }),
    getServerRpcQueue: () => ({
      add: (message: Record<string, unknown>) => queued.push(message),
      flush: () => {
        flushes++;
      }
    })
  };
  return { registry, queued, processed, flushes: () => flushes };
}

const node = (id: number) => ({ getId: () => id });

describe('ServerConnector', () => {
  it('sends an event message and enqueues + flushes it', () => {
    const { registry, queued, processed, flushes } = makeRegistry();
    new ServerConnector(registry).sendEventMessage(node(2), 'click', { x: 1 });
    expect(queued).to.deep.equal([{ type: 'event', node: 2, event: 'click', data: { x: 1 } }]);
    expect(processed).to.deep.equal([['event', 'click']]);
    expect(flushes()).to.equal(1);
  });

  it('omits event data when none is given', () => {
    const { registry, queued } = makeRegistry();
    new ServerConnector(registry).sendEventMessage(node(2), 'click', null);
    expect(queued[0]).to.not.have.property('data');
  });

  it('accepts a numeric node id (the published client API path)', () => {
    const { registry, queued } = makeRegistry();
    // ApplicationConnection.sendEventMessage / connectWebComponent pass the id.
    new ServerConnector(registry).sendEventMessage(2, 'click', { x: 1 });
    expect(queued).to.deep.equal([{ type: 'event', node: 2, event: 'click', data: { x: 1 } }]);
  });

  it('sends a node sync message with the encoded value', () => {
    const { registry, queued } = makeRegistry();
    new ServerConnector(registry).sendNodeSyncMessage(node(5), 1, 'title', 'hi');
    expect(queued[0]).to.deep.equal({ type: 'mSync', node: 5, feature: 1, property: 'title', value: 'hi' });
  });

  it('sends a template event message and includes the promise id only when set', () => {
    const { registry, queued } = makeRegistry();
    const connector = new ServerConnector(registry);
    connector.sendTemplateEventMessage(node(2), 'doIt', ['a'], -1);
    expect(queued[0]).to.not.have.property('promise');
    connector.sendTemplateEventMessage(node(2), 'doIt', ['a'], 0);
    expect(queued[1]).to.deep.include({ promise: 0 });
  });

  it('sends both existing-element attach variants', () => {
    const { registry, queued } = makeRegistry();
    const connector = new ServerConnector(registry);
    connector.sendExistingElementAttachToServer(node(1), 2, 3, 'div', 4);
    expect(queued[0]).to.deep.equal({
      type: 'attachExistingElement',
      node: 1,
      attachReqId: 2,
      attachAssignedId: 3,
      attachTagName: 'div',
      attachIndex: 4
    });

    connector.sendExistingElementWithIdAttachToServer(node(1), 2, 3, 'the-id');
    expect(queued[1]).to.deep.equal({
      type: 'attachExistingElementById',
      node: 1,
      attachReqId: 2,
      attachAssignedId: 3,
      attachId: 'the-id'
    });
  });
});
