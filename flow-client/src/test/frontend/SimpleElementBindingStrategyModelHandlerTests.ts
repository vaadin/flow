import { expect } from '@open-wc/testing';
import { UpdatableModelProperties } from '../../main/frontend/internal/model/UpdatableModelProperties';
import {
  handleListItemPropertyChange,
  handlePropertiesChanged,
  handlePropertyChange,
  InitialPropertyUpdate
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_PROPERTIES = 1;

// A StateNode stand-in for the model handlers: node data keyed by class plus a
// single ELEMENT_PROPERTIES map of scalar properties.
function fakeModelNode(config: {
  updatable?: UpdatableModelProperties | null;
  initialUpdate?: InitialPropertyUpdate | null;
  properties?: Record<string, { getValue(): unknown; syncToServer(value: unknown): void }>;
}): any {
  const properties = config.properties ?? {};
  const map = {
    hasPropertyValue: (name: string) => name in properties,
    getProperty: (name: string) => properties[name]
  };
  return {
    getNodeData: (clazz: unknown) => {
      if (clazz === UpdatableModelProperties) {
        return config.updatable ?? null;
      }
      if (clazz === InitialPropertyUpdate) {
        return config.initialUpdate ?? null;
      }
      return null;
    },
    getMap: () => map
  };
}

describe('SimpleElementBindingStrategy model handlers', () => {
  it('InitialPropertyUpdate runs the command once and clears itself', () => {
    const cleared: unknown[] = [];
    const ran: string[] = [];
    const update = new InitialPropertyUpdate({ clearNodeData: (o: object) => cleared.push(o) });
    update.setCommand(() => ran.push('run'));
    update.execute();
    expect(ran).to.deep.equal(['run']);
    expect(cleared).to.deep.equal([update]);
  });

  it('handleListItemPropertyChange syncs only when the node has element properties', () => {
    const synced: unknown[] = [];
    const node: any = {
      hasFeature: (feature: number) => feature === ELEMENT_PROPERTIES,
      getMap: () => ({ getProperty: () => ({ syncToServer: (v: unknown) => synced.push(v) }) })
    };
    handleListItemPropertyChange(5, null, 'value', 'x', { getNode: (id: number) => (id === 5 ? node : null) });
    expect(synced).to.deep.equal(['x']);

    // No ELEMENT_PROPERTIES feature => no sync.
    handleListItemPropertyChange(6, null, 'value', 'y', { getNode: () => ({ hasFeature: () => false }) } as any);
    expect(synced).to.deep.equal(['x']);
  });

  it('handlePropertyChange syncs an updatable scalar property', () => {
    const synced: unknown[] = [];
    const node = fakeModelNode({
      updatable: new UpdatableModelProperties(['name']),
      properties: { name: { getValue: () => 'old', syncToServer: (v) => synced.push(v) } }
    });
    handlePropertyChange('name', () => 'Bob', node);
    expect(synced).to.deep.equal(['Bob']);
  });

  it('handlePropertyChange ignores non-updatable properties and missing data', () => {
    const synced: unknown[] = [];
    const property = { getValue: () => 'old', syncToServer: (v: unknown) => synced.push(v) };

    handlePropertyChange(
      'name',
      () => 'Bob',
      fakeModelNode({ updatable: new UpdatableModelProperties([]), properties: { name: property } })
    );
    handlePropertyChange('name', () => 'Bob', fakeModelNode({ updatable: null, properties: { name: property } }));
    expect(synced).to.deep.equal([]);
  });

  it('handlePropertiesChanged runs immediately, or defers to the initial update', () => {
    const synced: unknown[] = [];
    const properties = { name: { getValue: () => 'old', syncToServer: (v: unknown) => synced.push(v) } };

    // No pending initial update => runs now.
    handlePropertiesChanged(
      { name: 'Bob' },
      fakeModelNode({ updatable: new UpdatableModelProperties(['name']), properties })
    );
    expect(synced).to.deep.equal(['Bob']);

    // Pending initial update => deferred until execute().
    synced.length = 0;
    const initialUpdate = new InitialPropertyUpdate({ clearNodeData: () => {} });
    handlePropertiesChanged(
      { name: 'Jane' },
      fakeModelNode({ updatable: new UpdatableModelProperties(['name']), initialUpdate, properties })
    );
    expect(synced).to.deep.equal([]);
    initialUpdate.execute();
    expect(synced).to.deep.equal(['Jane']);
  });
});
