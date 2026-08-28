import { expect } from '@open-wc/testing';
import { DependencyLoader } from '../../../../main/frontend/internal/client/DependencyLoader';
import { ResourceLoader } from '../../../../main/frontend/internal/client/ResourceLoader';
import type { ResourceLoadListener } from '../../../../main/frontend/internal/client/ResourceRegistry';

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

// Registries created by the case that is running, completed afterwards so the
// eager counter never leaks between cases.
const active: Array<{ completeAll(): void }> = [];

const eventLoader = new ResourceLoader({ getSystemErrorHandler: () => ({ handleError: () => {} }) }, false);

function makeRegistry() {
  const calls: Array<{ method: string; args: unknown[]; listener: ResourceLoadListener }> = [];
  const record =
    (method: string) =>
    (...args: unknown[]) => {
      const listener = args.find((a) => a && typeof a === 'object' && 'onLoad' in a) as ResourceLoadListener;
      calls.push({ method, args, listener });
    };
  const resourceLoader = {
    loadScript: record('loadScript'),
    loadJsModule: record('loadJsModule'),
    inlineScript: record('inlineScript'),
    loadStylesheet: record('loadStylesheet'),
    inlineStyleSheet: record('inlineStyleSheet'),
    loadDynamicImport: record('loadDynamicImport')
  };
  // Notifies every load started so far exactly once, the way a real
  // ResourceLoader eventually does; that is what balances the eager counter.
  const completeAll = (): void => {
    const pending = calls.splice(0, calls.length);
    for (const call of pending) {
      calls.push(call);
      // The event carries the loader that did the work; the recorder above
      // stands in for the loader's methods, so a real one is built for the
      // event itself.
      call.listener?.onLoad({ getResourceLoader: () => eventLoader, getResourceData: () => String(call.args[0]) });
    }
  };
  const registry = {
    calls,
    completeAll,
    getURIResolver: () => ({ resolveVaadinUri: (uri: string) => `resolved:${uri}` }),
    getResourceLoader: () => resourceLoader
  };
  active.push(registry);
  return registry;
}

// Ported from com.vaadin.client.DependencyLoaderTest and
// com.vaadin.client.GwtDependencyLoaderTest.
describe('DependencyLoader (class)', () => {
  afterEach(() => {
    active.splice(0, active.length).forEach((registry) => registry.completeAll());
  });

  it('loads an eager stylesheet via the resolved URL and the loadStylesheet method', () => {
    // Ported from loadStylesheet.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([['EAGER', [{ type: 'STYLESHEET', url: 'styles.css', id: 'dep-1' }]]])
    );
    const call = registry.calls.find((c) => c.method === 'loadStylesheet');
    expect(call).to.not.equal(undefined);
    expect(call?.args[0]).to.equal('resolved:styles.css');
    expect(call?.args[2]).to.equal('dep-1');
  });

  it('routes eager JavaScript to loadScript with defer=true', () => {
    // Ported from loadScript.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(new Map([['EAGER', [{ type: 'JAVASCRIPT', url: 'app.js' }]]]));
    const call = registry.calls.find((c) => c.method === 'loadScript');
    expect(call?.args).to.deep.equal(['resolved:app.js', call?.listener, false, true]);
  });

  it('routes inline JavaScript to inlineScript with the contents', () => {
    // Ported from inlineScript.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([['INLINE', [{ type: 'JAVASCRIPT', contents: 'window.x=1' }]]])
    );
    const call = registry.calls.find((c) => c.method === 'inlineScript');
    expect(call?.args[0]).to.equal('window.x=1');
  });

  it('routes a dynamic import to loadDynamicImport', () => {
    // Beyond the Java suite: the dynamic-import type postdates the Java tests.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([['LAZY', [{ type: 'DYNAMIC_IMPORT', url: 'import("x")' }]]])
    );
    // DYNAMIC_IMPORT is always eager, so it loads immediately.
    const call = registry.calls.find((c) => c.method === 'loadDynamicImport');
    expect(call?.args[0]).to.equal('import("x")');
  });

  it('defers lazy dependencies until after eager ones complete', async () => {
    // Ported from GwtDependencyLoaderTest.testEnsureLazyDependenciesLoadedInOrder.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([
        ['EAGER', [{ type: 'JAVASCRIPT', url: 'eager.js' }]],
        ['LAZY', [{ type: 'STYLESHEET', url: 'lazy.css' }]]
      ])
    );
    // The eager script was requested; the lazy one waits for it to finish.
    expect(registry.calls.some((c) => c.args[0] === 'resolved:eager.js')).to.be.true;
    await settle();
    expect(registry.calls.some((c) => c.args[0] === 'resolved:lazy.css')).to.be.false;

    // Complete the eager load, then let the deferred lazy loader run.
    registry.completeAll();
    await settle();
    expect(registry.calls.some((c) => c.args[0] === 'resolved:lazy.css')).to.be.true;
  });

  it('keeps eager dependencies in the order they were added', () => {
    // Ported from ensureEagerDependenciesLoadedInOrder and
    // GwtDependencyLoaderTest.testAllEagerDependenciesAreLoadedFirst.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([
        [
          'EAGER',
          [
            { type: 'JAVASCRIPT', url: '/1.js' },
            { type: 'JAVASCRIPT', url: '/2.js' },
            { type: 'STYLESHEET', url: '/1.css' },
            { type: 'STYLESHEET', url: '/2.css' }
          ]
        ]
      ])
    );

    const urlsFor = (method: string): unknown[] =>
      registry.calls.filter((c) => c.method === method).map((c) => c.args[0]);
    expect(urlsFor('loadScript')).to.deep.equal(['resolved:/1.js', 'resolved:/2.js']);
    expect(urlsFor('loadStylesheet')).to.deep.equal(['resolved:/1.css', 'resolved:/2.css']);
  });

  it('keeps inline dependencies in the order they were added', () => {
    // Ported from ensureInlineDependenciesLoadedInOrder.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([
        [
          'INLINE',
          [
            { type: 'JAVASCRIPT', contents: '/1.js' },
            { type: 'JAVASCRIPT', contents: '/2.js' },
            { type: 'STYLESHEET', contents: '/1.css' },
            { type: 'STYLESHEET', contents: '/2.css' }
          ]
        ]
      ])
    );

    const contentsFor = (method: string): unknown[] =>
      registry.calls.filter((c) => c.method === method).map((c) => c.args[0]);
    expect(contentsFor('inlineScript')).to.deep.equal(['/1.js', '/2.js']);
    expect(contentsFor('inlineStyleSheet')).to.deep.equal(['/1.css', '/2.css']);
  });

  it('loads several eager dependencies of different types', () => {
    // Ported from loadMultiple.
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([
        [
          'EAGER',
          [
            { type: 'JAVASCRIPT', url: 'http://foo.bar/baz.js' },
            { type: 'JAVASCRIPT', url: '/my.js' },
            { type: 'STYLESHEET', url: 'https://x.yz/styles.css' }
          ]
        ]
      ])
    );

    const urlsFor = (method: string): unknown[] =>
      registry.calls.filter((c) => c.method === method).map((c) => c.args[0]);
    expect(urlsFor('loadScript')).to.deep.equal(['resolved:http://foo.bar/baz.js', 'resolved:/my.js']);
    expect(urlsFor('loadStylesheet')).to.deep.equal(['resolved:https://x.yz/styles.css']);
  });

  it('rejects inline JsModule', () => {
    // Beyond the Java suite: the JsModule inline guard has no Java case.
    const registry = makeRegistry();
    expect(() =>
      new DependencyLoader(registry).loadDependencies(new Map([['INLINE', [{ type: 'JS_MODULE', contents: 'x' }]]]))
    ).to.throw('Inline load mode is not supported for JsModule');
  });
});
