import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import {
  bindMap,
  bindProperty,
  createComputations
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

// A NodeMap stand-in over a fixed set of named properties, with a hook to fire a
// property-add event.
function fakeMap(properties: Array<{ getName(): string }>) {
  const addListeners: Array<(event: { getProperty(): any }) => void> = [];
  return {
    forEachProperty: (cb: (property: any, name: string) => void) => properties.forEach((p) => cb(p, p.getName())),
    addPropertyAddListener(listener: (event: { getProperty(): any }) => void) {
      addListeners.push(listener);
      return { remove: () => addListeners.splice(addListeners.indexOf(listener), 1) };
    },
    fireAdd(property: { getName(): string }) {
      addListeners.forEach((l) => l({ getProperty: () => property }));
    }
  };
}

describe('SimpleElementBindingStrategy map/property binding', () => {
  afterEach(() => Reactive.flush());

  it('createComputations tracks and returns a fresh map', () => {
    const collection: Array<Map<string, any>> = [];
    const computations = createComputations(collection);
    expect(collection).to.have.length(1);
    expect(collection[0]).to.equal(computations);
  });

  it('bindProperty registers a computation that runs the user on recompute', () => {
    const used: string[] = [];
    const bindings = new Map<string, any>();
    const property = { getName: () => 'color' };
    const computation = bindProperty((p: { getName(): string }) => used.push(p.getName()), property, bindings);
    expect(bindings.get('color')).to.equal(computation);
    computation.recompute();
    expect(used).to.deep.equal(['color']);
  });

  it('bindMap applies existing properties eagerly and added ones on flush', () => {
    const used: string[] = [];
    const user = (p: { getName(): string }) => used.push(p.getName());
    const a = { getName: () => 'a' };
    const b = { getName: () => 'b' };
    const map = fakeMap([a]);
    const bindings = new Map<string, any>();

    const remover = bindMap(1, user, bindings, { getMap: () => map });
    // Existing property bound and applied eagerly.
    expect(used).to.deep.equal(['a']);
    expect(bindings.has('a')).to.be.true;

    // Added property is applied on the next flush.
    map.fireAdd(b);
    Reactive.flush();
    expect(used).to.deep.equal(['a', 'b']);
    expect(bindings.has('b')).to.be.true;

    // The returned remover detaches the add listener.
    remover.remove();
    map.fireAdd({ getName: () => 'c' });
    Reactive.flush();
    expect(used).to.deep.equal(['a', 'b']);
  });
});
