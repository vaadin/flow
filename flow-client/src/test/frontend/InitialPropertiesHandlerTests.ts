import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { InitialPropertiesHandler } from '../../main/frontend/internal/InitialPropertiesHandler';

// com.vaadin.flow.internal.nodefeature.NodeFeatures.ELEMENT_PROPERTIES
const ELEMENT_PROPERTIES = 1;

// A StateNode stand-in whose ELEMENT_PROPERTIES feature holds the given initial
// server property values (undefined => no ELEMENT_PROPERTIES feature).
function fakeNode(id: number, initialProps?: Record<string, unknown>): any {
  return {
    getId: () => id,
    hasFeature: (feature: number) => feature === ELEMENT_PROPERTIES && initialProps !== undefined,
    getMap: (_feature: number) => ({
      forEachProperty: (cb: (property: any, name: string) => void) =>
        Object.entries(initialProps ?? {}).forEach(([name, value]) =>
          cb({ getName: () => name, getValue: () => value }, name)
        )
    })
  };
}

// A MapProperty stand-in recording setValue calls.
function fakeProperty(node: any, name: string, value: unknown) {
  const setCalls: unknown[] = [];
  return {
    setCalls,
    getName: () => name,
    getValue: () => value,
    setValue: (v: unknown) => setCalls.push(v),
    getMap: () => ({ getNode: () => node })
  };
}

function fakeRegistry(nodesById: Record<number, any>, updateInProgress: boolean) {
  const sent: string[] = [];
  const tree = {
    sent,
    isUpdateInProgress: () => updateInProgress,
    getNode: (id: number) => nodesById[id] ?? null,
    sendNodePropertySyncToServer: (property: any) => sent.push(property.getName())
  };
  return { tree, registry: { getStateTree: () => tree } };
}

describe('InitialPropertiesHandler', () => {
  it('queues property updates only for newly created nodes', () => {
    const node = fakeNode(2, {});
    const { registry } = fakeRegistry({ 2: node }, false);
    const handler = new InitialPropertiesHandler(registry);

    // Not registered yet => handled normally by the caller.
    expect(handler.handlePropertyUpdate(fakeProperty(node, 'foo', 'x'))).to.be.false;

    handler.nodeRegistered(node);
    expect(handler.handlePropertyUpdate(fakeProperty(node, 'foo', 'x'))).to.be.true;
  });

  it('does nothing while a server update is in progress', () => {
    const node = fakeNode(2, { color: 'red' });
    const { tree, registry } = fakeRegistry({ 2: node }, true);
    const handler = new InitialPropertiesHandler(registry);

    handler.nodeRegistered(node);
    const property = fakeProperty(node, 'color', 'blue');
    handler.handlePropertyUpdate(property);
    handler.flushPropertyUpdates();
    Reactive.flush();

    expect(tree.sent).to.deep.equal([]);
    expect(property.setCalls).to.deep.equal([]);
  });

  it('resets properties with a server initial value and sends the rest', () => {
    const node = fakeNode(2, { color: 'red' });
    const { tree, registry } = fakeRegistry({ 2: node }, false);
    const handler = new InitialPropertiesHandler(registry);

    handler.nodeRegistered(node);
    const colorProperty = fakeProperty(node, 'color', 'blue');
    const sizeProperty = fakeProperty(node, 'size', 'L');
    handler.handlePropertyUpdate(colorProperty);
    handler.handlePropertyUpdate(sizeProperty);

    handler.flushPropertyUpdates();
    Reactive.flush();

    // 'color' had a server initial value => reset to it, not sent.
    expect(colorProperty.setCalls).to.deep.equal(['red']);
    // 'size' had no server initial value => sent to the server.
    expect(tree.sent).to.deep.equal(['size']);
  });
});
