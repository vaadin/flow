import { expect } from '@open-wc/testing';
import { Computation, Reactive } from '../../main/frontend/internal/reactive/reactive';
import {
  MapProperty,
  type MapPropertyChangeEvent,
  type MapPropertyNode,
  type MapPropertyOwner,
  type MapPropertyTree
} from '../../main/frontend/internal/nodefeature/MapProperty';

// Builds a MapProperty backed by mock state-tree contracts, recording the
// properties synced to the server.
function makeProperty(active = true, forceValueUpdate = false): { property: MapProperty; synced: MapProperty[] } {
  const synced: MapProperty[] = [];
  const tree: MapPropertyTree = {
    isActive: () => active,
    sendNodePropertySyncToServer: (p) => synced.push(p)
  };
  const node: MapPropertyNode = { getTree: () => tree };
  const map: MapPropertyOwner = { getNode: () => node };
  return { property: new MapProperty('foo', map, forceValueUpdate), synced };
}

// Mirrors the Java CountingComputation helper.
function countingComputation(reader: () => void): { computation: Computation; getCount: () => number } {
  let count = 0;
  const computation = new Computation(() => {
    count++;
    reader();
  });
  return { computation, getCount: () => count };
}

describe('MapProperty', () => {
  let property: MapProperty;
  beforeEach(() => {
    Reactive.reset();
    property = makeProperty().property;
  });

  it('holds a value once set', () => {
    expect(property.getValue()).to.equal(null);
    expect(property.hasValue()).to.equal(false);
    property.setValue('bar');
    expect(property.getValue()).to.equal('bar');
    expect(property.hasValue()).to.equal(true);
  });

  it('fires change events on real changes only', () => {
    const lastEvent: { value: MapPropertyChangeEvent | null } = { value: null };
    const remover = property.addChangeListener((event) => {
      expect(lastEvent.value, 'Got unexpected event').to.equal(null);
      lastEvent.value = event;
    });

    property.setValue('foo');
    const event = lastEvent.value;
    expect(event).to.not.equal(null);
    expect(event!.getSource()).to.equal(property);
    expect(event!.getOldValue()).to.equal(null);
    expect(event!.getNewValue()).to.equal('foo');

    property.setValue('foo');
    expect(lastEvent.value).to.equal(event);

    lastEvent.value = null;
    property.removeValue();
    const removeEvent = lastEvent.value;
    expect(removeEvent!.getNewValue()).to.equal(null);

    property.removeValue();
    expect(lastEvent.value).to.equal(removeEvent);

    lastEvent.value = null;
    property.setValue(null);
    const addBackEvent = lastEvent.value;
    expect(addBackEvent!.getOldValue()).to.equal(null);

    remover.remove();
    property.setValue('bar');
    expect(lastEvent.value).to.equal(addBackEvent);
  });

  it('recomputes a value reader only on flush after change', () => {
    const { getCount } = countingComputation(() => property.getValue());
    Reactive.flush();
    expect(getCount()).to.equal(1);
    property.setValue('bar');
    property.setValue('baz');
    expect(getCount()).to.equal(1);
    Reactive.flush();
    expect(getCount()).to.equal(2);
  });

  it('recomputes a hasValue reader on flush after change', () => {
    const { getCount } = countingComputation(() => property.hasValue());
    Reactive.flush();
    property.setValue('baz');
    expect(getCount()).to.equal(1);
    Reactive.flush();
    expect(getCount()).to.equal(2);
  });

  it('removes its value', () => {
    property.setValue('foo');
    expect(property.hasValue()).to.equal(true);
    property.removeValue();
    expect(property.hasValue()).to.equal(false);
    expect(property.getValue()).to.equal(null);
  });

  it('getValueOrDefault for numbers', () => {
    expect(property.getValueOrDefault(12)).to.equal(12);
    property.setValue(24.0);
    expect(property.getValueOrDefault(12)).to.equal(24);
    property.setValue(null);
    expect(property.getValueOrDefault(12)).to.equal(12);
    property.removeValue();
    expect(property.getValueOrDefault(12)).to.equal(12);
  });

  it('getValueOrDefault for booleans', () => {
    expect(property.getValueOrDefault(true)).to.equal(true);
    expect(property.getValueOrDefault(false)).to.equal(false);
    property.setValue(true);
    expect(property.getValueOrDefault(false)).to.equal(true);
    property.setValue(null);
    expect(property.getValueOrDefault(true)).to.equal(true);
    expect(property.getValueOrDefault(false)).to.equal(false);
    property.removeValue();
    expect(property.getValueOrDefault(true)).to.equal(true);
    expect(property.getValueOrDefault(false)).to.equal(false);
  });

  it('getValueOrDefault for strings', () => {
    expect(property.getValueOrDefault('default')).to.equal('default');
    property.setValue('assigned');
    expect(property.getValueOrDefault('default')).to.equal('assigned');
    property.setValue(null);
    expect(property.getValueOrDefault('default')).to.equal('default');
    property.removeValue();
    expect(property.getValueOrDefault('default')).to.equal('default');
  });

  it('syncToServer sends the property when the node is active', () => {
    const { property: active, synced } = makeProperty(true);
    active.syncToServer('bar');
    expect(active.getValue()).to.equal('bar');
    expect(synced).to.deep.equal([active]);
  });

  it('syncToServer does not send when the node is inactive, fires an event and flushes', () => {
    const { property: inactive, synced } = makeProperty(false);

    const capture: { value: MapPropertyChangeEvent | null } = { value: null };
    inactive.addChangeListener((event) => {
      capture.value = event;
    });

    const flushListener = { ran: false };
    Reactive.addFlushListener(() => {
      flushListener.ran = true;
    });

    inactive.syncToServer('bar');

    expect(synced).to.deep.equal([]);
    expect(capture.value).to.not.equal(null);
    expect(capture.value!.getNewValue()).to.equal(null);
    expect(flushListener.ran).to.equal(true);
  });

  it('setValue then syncToServer without flush does not update the value', () => {
    const { property: p } = makeProperty(true);
    p.setValue('bar');
    p.syncToServer('baz');
    expect(p.getValue()).to.equal('bar');
  });

  it('setValue, flush, then syncToServer updates the value', () => {
    const { property: p } = makeProperty(true);
    p.setValue('bar');
    Reactive.flush();
    p.syncToServer('baz');
    expect(p.getValue()).to.equal('baz');
  });

  it('setValue then syncToServer twice updates the value', () => {
    const { property: p } = makeProperty(true);
    p.setValue('bar');
    p.syncToServer('bar');
    p.syncToServer('baz');
    expect(p.getValue()).to.equal('baz');
  });

  it('removeValue then syncToServer without flush does not update the value', () => {
    const { property: p } = makeProperty(true);
    p.setValue('bar');
    p.removeValue();
    p.syncToServer('baz');
    expect(p.getValue()).to.equal(null);
  });

  it('removeValue, flush, then syncToServer updates the value', () => {
    const { property: p } = makeProperty(true);
    p.setValue('bar');
    p.removeValue();
    Reactive.flush();
    p.syncToServer('baz');
    expect(p.getValue()).to.equal('baz');
  });

  it('removeValue then syncToServer twice updates the value', () => {
    const { property: p } = makeProperty(true);
    p.setValue('bar');
    p.removeValue();
    p.syncToServer(null);
    p.syncToServer('baz');
    expect(p.getValue()).to.equal('baz');
  });

  it('syncToServer syncs a property that has no value', () => {
    const { property: p, synced } = makeProperty(true);
    p.syncToServer(null);
    expect(synced).to.deep.equal([p]);
  });

  it('setValue with forced update fires an event every time', () => {
    const { property: p } = makeProperty(true, true);

    const capture: { value: MapPropertyChangeEvent | null } = { value: null };
    p.addChangeListener((event) => {
      capture.value = event;
    });

    p.setValue('bar');
    expect(capture.value).to.not.equal(null);

    capture.value = null;
    // set a different value again
    p.setValue('foo');
    expect(capture.value).to.not.equal(null);
  });

  it('setValue with default strategy fires an event only once for the same value', () => {
    const { property: p } = makeProperty(true, false);

    const capture: { value: MapPropertyChangeEvent | null } = { value: null };
    p.addChangeListener((event) => {
      capture.value = event;
    });

    p.setValue('bar');
    expect(capture.value).to.not.equal(null);

    capture.value = null;
    // set the same value again
    p.setValue('bar');
    expect(capture.value).to.equal(null);
  });
});
