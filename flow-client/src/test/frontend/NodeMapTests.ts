import { expect } from '@open-wc/testing';
import { Computation, Reactive } from '../../main/frontend/internal/reactive/reactive';
import type { MapProperty, MapPropertyChangeEvent } from '../../main/frontend/internal/nodefeature/MapProperty';
import { NodeMap, type MapPropertyAddEvent } from '../../main/frontend/internal/nodefeature/NodeMap';
import type { NodeFeatureNode } from '../../main/frontend/internal/nodefeature/NodeFeature';

// Minimal StateNode stand-in; node-feature tests do not reach into the tree.
const node: NodeFeatureNode = {
  getTree: () => {
    throw new Error('tree not available in this test');
  },
  getDebugJson: () => null
};

function countingComputation(reader: () => void): { getCount: () => number } {
  let count = 0;
  void new Computation(() => {
    count++;
    reader();
  });
  return { getCount: () => count };
}

describe('NodeMap', () => {
  let map: NodeMap;
  beforeEach(() => {
    Reactive.reset();
    map = new NodeMap(0, node);
  });

  it('is initially empty', () => {
    map.forEachProperty(() => {
      throw new Error('should be empty');
    });
  });

  it('creates and reuses properties', () => {
    const property = map.getProperty('foo');
    expect(property.getName()).to.equal('foo');
    expect(property.getMap()).to.equal(map);

    const collected: MapProperty[] = [];
    map.forEachProperty((p) => collected.push(p));
    expect(collected).to.deep.equal([property]);

    const getAgain = map.getProperty('foo');
    expect(getAgain).to.equal(property);
  });

  it('fires an event when a property is added', () => {
    const lastEvent: { value: MapPropertyAddEvent | null } = { value: null };
    const remover = map.addPropertyAddListener((event) => {
      expect(lastEvent.value, 'Got unexpected event').to.equal(null);
      lastEvent.value = event;
    });

    expect(lastEvent.value).to.equal(null);

    map.getProperty('foo');
    const event = lastEvent.value;
    expect(event!.getSource()).to.equal(map);
    expect(event!.getProperty().getName()).to.equal('foo');

    lastEvent.value = null;
    map.getProperty('foo');
    expect(lastEvent.value).to.equal(null);

    map.getProperty('bar');
    expect(lastEvent.value!.getProperty().getName()).to.equal('bar');

    remover.remove();
    map.getProperty('baz');
    expect(lastEvent.value!.getProperty().getName()).to.equal('bar');
  });

  it('invalidates a property iteration when a property is added', () => {
    const { getCount } = countingComputation(() => map.forEachProperty(() => {}));
    Reactive.flush();
    expect(getCount()).to.equal(1);
    map.getProperty('foo');
    expect(getCount()).to.equal(1);
    Reactive.flush();
    expect(getCount()).to.equal(2);
  });

  it('hasPropertyValue is false for a non-existing property and does not create it', () => {
    expect(map.hasPropertyValue('foo')).to.equal(false);
    map.forEachProperty(() => {
      throw new Error('there should be no properties');
    });
  });

  it('hasPropertyValue reflects whether the property has a value', () => {
    map.getProperty('foo');
    expect(map.hasPropertyValue('foo')).to.equal(false);
    map.getProperty('foo').setValue('bar');
    expect(map.hasPropertyValue('foo')).to.equal(true);
    map.getProperty('foo').removeValue();
    expect(map.hasPropertyValue('foo')).to.equal(false);
  });

  it('innerHTML on the element-properties map always updates the value', () => {
    const elementProperties = new NodeMap(1, node);
    const property = elementProperties.getProperty('innerHTML');

    const capture: { value: MapPropertyChangeEvent | null } = { value: null };
    property.addChangeListener((event) => {
      capture.value = event;
    });

    property.setValue('foo');
    expect(capture.value).to.not.equal(null);

    capture.value = null;
    property.setValue('foo');
    expect(capture.value).to.not.equal(null);
  });
});
