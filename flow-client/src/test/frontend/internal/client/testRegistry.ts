// A real Registry a suite can fill with as much or as little as it needs.
//
// Registry.set is protected, as in Java, so a suite cannot register a service on
// a Registry instance from outside. This subclass opens exactly that, keyed by
// the same tokens DefaultRegistry uses, so a suite hands the code under test the
// real registry type instead of an object literal cast into it. Services the
// suite does not register stay unregistered, and looking one up throws.

import { Registry, TOKEN } from '../../../../main/frontend/internal/client/Registry';

/** The service names a suite can register, i.e. the registry's own tokens. */
export type ServiceName = keyof typeof TOKEN;

/** A registry a suite fills in, service by service. */
export class TestRegistry extends Registry {
  /**
   * Registers the instance to answer the getter for the given service.
   *
   * @param name - the service to register
   * @param instance - the instance (or fake) to hand out
   * @returns this registry, so registrations can be chained
   */
  register(name: ServiceName, instance: unknown): this {
    this.set(TOKEN[name], instance);
    return this;
  }
}

/**
 * A registry holding the given services and nothing else.
 *
 * @param services - the instances (or fakes) to register, by service name
 * @returns the registry
 */
export function testRegistry(services: Partial<Record<ServiceName, unknown>>): TestRegistry {
  const registry = new TestRegistry();
  for (const [name, instance] of Object.entries(services)) {
    registry.register(name as ServiceName, instance);
  }
  return registry;
}
