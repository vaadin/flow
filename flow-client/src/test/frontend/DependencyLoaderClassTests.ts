import { expect } from '@open-wc/testing';
import { DependencyLoader } from '../../main/frontend/internal/DependencyLoader';
import { resetForTesting } from '../../main/frontend/internal/EagerDependencyTracker';
import type { ResourceLoadListener } from '../../main/frontend/internal/ResourceRegistry';

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

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
    loadDynamicImport: record('loadDynamicImport'),
    runWhenHtmlImportsReady: (task: () => void) => task()
  };
  const registry = {
    calls,
    getURIResolver: () => ({ resolveVaadinUri: (uri: string) => `resolved:${uri}` }),
    getResourceLoader: () => resourceLoader
  };
  return registry;
}

describe('DependencyLoader (class)', () => {
  beforeEach(() => resetForTesting());

  it('loads an eager stylesheet via the resolved URL and the loadStylesheet method', () => {
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
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(new Map([['EAGER', [{ type: 'JAVASCRIPT', url: 'app.js' }]]]));
    const call = registry.calls.find((c) => c.method === 'loadScript');
    expect(call?.args).to.deep.equal(['resolved:app.js', call?.listener, false, true]);
  });

  it('routes inline JavaScript to inlineScript with the contents', () => {
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([['INLINE', [{ type: 'JAVASCRIPT', contents: 'window.x=1' }]]])
    );
    const call = registry.calls.find((c) => c.method === 'inlineScript');
    expect(call?.args[0]).to.equal('window.x=1');
  });

  it('routes a dynamic import to loadDynamicImport', () => {
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([['LAZY', [{ type: 'DYNAMIC_IMPORT', url: 'import("x")' }]]])
    );
    // DYNAMIC_IMPORT is always eager, so it loads immediately.
    const call = registry.calls.find((c) => c.method === 'loadDynamicImport');
    expect(call?.args[0]).to.equal('import("x")');
  });

  it('defers lazy dependencies until after eager ones complete', async () => {
    const registry = makeRegistry();
    new DependencyLoader(registry).loadDependencies(
      new Map([
        ['EAGER', [{ type: 'JAVASCRIPT', url: 'eager.js' }]],
        ['LAZY', [{ type: 'STYLESHEET', url: 'lazy.css' }]]
      ])
    );
    // Eager loaded right away; lazy not yet (eager still in flight).
    expect(registry.calls.some((c) => c.args[0] === 'resolved:lazy.css')).to.be.false;

    // Complete the eager load, then let the deferred lazy loader run.
    registry.calls.find((c) => c.method === 'loadScript')?.listener.onLoad({} as never);
    await settle();
    expect(registry.calls.some((c) => c.args[0] === 'resolved:lazy.css')).to.be.true;
  });

  it('rejects inline JsModule', () => {
    const registry = makeRegistry();
    expect(() =>
      new DependencyLoader(registry).loadDependencies(new Map([['INLINE', [{ type: 'JS_MODULE', contents: 'x' }]]]))
    ).to.throw('Inline load mode is not supported for JsModule');
  });
});
