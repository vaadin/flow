import { unavailableRegistry } from '../stateTreeTestRegistry';
import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { countingComputation } from '../reactive/CountingComputation';
import type { MapProperty } from '../../../../../../main/frontend/internal/client/flow/nodefeature/MapProperty';
import type { MapPropertyChangeEvent } from '../../../../../../main/frontend/internal/client/flow/nodefeature/MapPropertyChangeEvent';
import { NodeMap } from '../../../../../../main/frontend/internal/client/flow/nodefeature/NodeMap';
import type { MapPropertyAddEvent } from '../../../../../../main/frontend/internal/client/flow/nodefeature/MapPropertyAddEvent';
import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../../main/frontend/internal/client/flow/StateTree';

// A real state node; node-feature tests do not reach into the tree.
const node = new StateNode(0, new StateTree(unavailableRegistry()));

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

  it('hasPropertyValue is false for an existing property without a value', () => {
    map.getProperty('foo');
    expect(map.hasPropertyValue('foo')).to.equal(false);
  });

  it('hasPropertyValue is true for an existing property with a value', () => {
    map.getProperty('foo').setValue('bar');
    expect(map.hasPropertyValue('foo')).to.equal(true);
  });

  it('hasPropertyValue is false after removing the value', () => {
    const p = map.getProperty('foo');
    p.setValue('bar');
    expect(map.hasPropertyValue('foo')).to.equal(true);
    p.removeValue();
    expect(map.hasPropertyValue('foo')).to.equal(false);
  });

  it('keeps the prototype methods of an object stored as a property value', () => {
    // Ported from GwtBasicElementBinderTest.testPropertyValueHasPrototypeMethods:
    // an object stored as a property value is a real object, so it still carries
    // its prototype methods (e.g. toString).
    const object = { name: 'bar' };
    map.getProperty('foo').setValue(object);
    expect(map.hasPropertyValue('foo')).to.equal(true);
    expect(String(map.getProperty('foo').getValue())).to.equal('[object Object]');
  });

  it('innerHTML on the element-properties map always updates the value', () => {
    const elementProperties = new NodeMap(NodeFeatures.ELEMENT_PROPERTIES, node);
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
