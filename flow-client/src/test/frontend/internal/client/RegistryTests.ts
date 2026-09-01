import { expect } from '@open-wc/testing';
import type { ServiceKey } from '../../../../main/frontend/internal/client/Registry';
import { TestRegistry } from './testRegistry';

// The container cases store and look up arbitrary tokens, which the shared
// TestRegistry does not: it registers the registry's own services. This subclass
// adds the rest of the protected container API those cases drive. Java's
// RegistryTest calls set/get directly, as a class in the same package may.
class TokenRegistry extends TestRegistry {
  registerToken<T>(type: ServiceKey, instance: T): void {
    this.set(type, instance);
  }

  registerResettable<T>(type: ServiceKey, supplier: () => T): void {
    this.setResettable(type, supplier);
  }

  lookup<T>(type: ServiceKey): T {
    return this.get(type);
  }
}

// Ported from com.vaadin.client.RegistryTest.
describe('Registry', () => {
  it('stores and looks up an instance by token', () => {
    // Ported from setAndGet.
    const registry = new TokenRegistry();
    const service = { name: 'sender' };
    registry.registerToken('MessageSender', service);
    expect(registry.lookup('MessageSender')).to.equal(service);
  });

  it('stores and looks up an instance by its class', () => {
    // Ported from setAndGetCustom. Java keys by Class; the port's key is an
    // opaque token, so a class constructor stands in for one here.
    class MyClass {
      readonly marker = 'my-class';
    }
    const registry = new TokenRegistry();
    const instance = new MyClass();
    registry.registerToken(MyClass, instance);
    expect(registry.lookup(MyClass)).to.equal(instance);
  });

  it('throws when registering the same type twice', () => {
    // Beyond the Java suite.
    const registry = new TokenRegistry();
    registry.registerToken('X', {});
    // Java interpolates the class name into the message; the token carries it.
    expect(() => registry.registerToken('X', {})).to.throw('already has a class of type X');
  });

  it('throws when looking up an unregistered type', () => {
    // Ported from getUndefined.
    const registry = new TokenRegistry();
    expect(() => registry.lookup('missing')).to.throw(
      'Tried to lookup type missing but no instance has been registered'
    );
  });

  it('recreates resettable instances on reset, leaving final ones untouched', () => {
    // Beyond the Java suite: Registry.java has no resettable-supplier overload.
    const registry = new TokenRegistry();
    const final = { id: 'final' };
    registry.registerToken('Final', final);

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
