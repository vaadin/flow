import { expect } from '@open-wc/testing';
import {
  isPolymerMicroLoaded,
  resetForTesting,
  updateApiImplementation,
  wrap
} from '../../main/frontend/internal/dom/DomApi';

type Win = { Polymer?: { dom(node: Node): unknown; version: string } };

function setPolymer(version: string, dom: (node: Node) => unknown = (node) => node): void {
  (window as Win).Polymer = { version, dom };
}

function clearPolymer(): void {
  delete (window as Win).Polymer;
}

describe('DomApi', () => {
  afterEach(() => {
    resetForTesting();
    clearPolymer();
  });

  it('wrap returns the node itself when no implementation is set', () => {
    const element = document.createElement('div');
    expect(wrap(element)).to.equal(element);
  });

  it('isPolymerMicroLoaded is true only for Polymer 1.x', () => {
    expect(isPolymerMicroLoaded()).to.be.false;
    setPolymer('1.9.1');
    expect(isPolymerMicroLoaded()).to.be.true;
    setPolymer('2.0.2');
    expect(isPolymerMicroLoaded()).to.be.false;
  });

  it('updateApiImplementation switches to the Polymer DOM API once micro is loaded', () => {
    const element = document.createElement('div');
    const wrapped = { sentinel: true };
    setPolymer('1.9.1', () => wrapped);

    // Before the update, wrap is still the identity.
    expect(wrap(element)).to.equal(element);

    updateApiImplementation();
    // After the update, wrap delegates to Polymer.dom.
    expect(wrap(element)).to.equal(wrapped);
  });

  it('updateApiImplementation does nothing for Polymer 2', () => {
    const element = document.createElement('div');
    setPolymer('2.0.2', () => ({ sentinel: true }));
    updateApiImplementation();
    expect(wrap(element)).to.equal(element);
  });

  it('the Polymer implementation stays in use even if it loads only once', () => {
    const element = document.createElement('div');
    const wrapped = { sentinel: true };
    setPolymer('1.9.1', () => wrapped);
    updateApiImplementation();
    // A second update is a no-op and keeps the Polymer implementation.
    updateApiImplementation();
    expect(wrap(element)).to.equal(wrapped);
  });
});
