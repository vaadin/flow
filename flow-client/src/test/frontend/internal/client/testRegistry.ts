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

// What the registry's getter for a service returns, i.e. the ported class or
// interface a fake for it stands in for.
type Service<N extends ServiceName> = Registry[`get${N}`] extends () => infer T ? T : never;

// A fake may implement only the members the code under test calls, but every
// member it does implement is checked against the real one, so a fake cannot
// drift from the class it stands in for.
type Fake<N extends ServiceName> = Partial<Service<N>>;

/** The services to register, by service name. */
export type TestServices = { [N in ServiceName]?: Fake<N> };

/** A registry a suite fills in, service by service. */
export class TestRegistry extends Registry {
  /**
   * Registers the instance to answer the getter for the given service.
   *
   * @param name - the service to register
   * @param instance - the instance, or a fake of the members under test
   * @returns this registry, so registrations can be chained
   * @typeParam N - the service being registered
   */
  register<N extends ServiceName>(name: N, instance: Fake<N>): this {
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
export function testRegistry(services: TestServices): TestRegistry {
  const registry = new TestRegistry();
  for (const [name, instance] of Object.entries(services)) {
    registry.register(name as ServiceName, instance as Fake<ServiceName>);
  }
  return registry;
}
