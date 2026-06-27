import { expect } from '@open-wc/testing';
import { Registry, type ServiceKey } from '../../main/frontend/internal/Registry';

// Test subclass exposing the protected container API (the cutover subclass will
// instead register concrete services and add typed getters).
class TestRegistry extends Registry {
  register<T>(type: ServiceKey, instance: T): void {
    this.set(type, instance);
  }

  registerResettable<T>(type: ServiceKey, supplier: () => T): void {
    this.setResettable(type, supplier);
  }

  lookup<T>(type: ServiceKey): T {
    return this.get(type);
  }

  contains(type: ServiceKey): boolean {
    return this.has(type);
  }
}

describe('Registry', () => {
  it('stores and looks up an instance by token', () => {
    const registry = new TestRegistry();
    const service = { name: 'sender' };
    registry.register('MessageSender', service);
    expect(registry.contains('MessageSender')).to.be.true;
    expect(registry.lookup('MessageSender')).to.equal(service);
  });

  it('throws when registering the same type twice', () => {
    const registry = new TestRegistry();
    registry.register('X', {});
    expect(() => registry.register('X', {})).to.throw('already has');
  });

  it('throws when looking up an unregistered type', () => {
    const registry = new TestRegistry();
    expect(() => registry.lookup('missing')).to.throw('no instance has been registered');
  });

  it('recreates resettable instances on reset, leaving final ones untouched', () => {
    const registry = new TestRegistry();
    const final = { id: 'final' };
    registry.register('Final', final);

    let counter = 0;
    registry.registerResettable('Resettable', () => ({ id: counter++ }));
    const first = registry.lookup<{ id: number }>('Resettable');
    expect(first.id).to.equal(0);

    registry.reset();
    const second = registry.lookup<{ id: number }>('Resettable');
    expect(second.id).to.equal(1);
    expect(second).to.not.equal(first);
    // Non-resettable instance is unchanged.
    expect(registry.lookup('Final')).to.equal(final);
  });
});
