import { expect } from '@open-wc/testing';
import { type ResourceLoadEvent, ResourceRegistry } from '../../main/frontend/internal/ResourceRegistry';

function event(resourceData: string): ResourceLoadEvent {
  return { getResourceLoader: () => null, getResourceData: () => resourceData };
}

function recordingListener() {
  const calls: string[] = [];
  return {
    calls,
    listener: {
      onLoad: () => calls.push('load'),
      onError: () => calls.push('error')
    }
  };
}

describe('ResourceRegistry', () => {
  it('reports the first listener for a key, but not later ones', () => {
    const registry = new ResourceRegistry({ handleError: () => {} });
    expect(registry.addListener('a.js', recordingListener().listener)).to.be.true;
    expect(registry.addListener('a.js', recordingListener().listener)).to.be.false;
  });

  it('fires load to all listeners, marks loaded, and clears them', () => {
    const errors: string[] = [];
    const registry = new ResourceRegistry({ handleError: (m) => errors.push(m) });
    const a = recordingListener();
    const b = recordingListener();
    registry.addListener('a.js', a.listener);
    registry.addListener('a.js', b.listener);

    registry.fireLoad(event('a.js'));
    expect(a.calls).to.deep.equal(['load']);
    expect(b.calls).to.deep.equal(['load']);
    expect(registry.isLoaded('a.js')).to.be.true;
    expect(errors).to.deep.equal([]);

    // Listeners were cleared: a second fire notifies nobody.
    a.calls.length = 0;
    registry.fireLoad(event('a.js'));
    expect(a.calls).to.deep.equal([]);
  });

  it('fires error to all listeners and reports it, without marking loaded', () => {
    const errors: string[] = [];
    const registry = new ResourceRegistry({ handleError: (m) => errors.push(m) });
    const a = recordingListener();
    registry.addListener('bad.js', a.listener);

    registry.fireError(event('bad.js'));
    expect(a.calls).to.deep.equal(['error']);
    expect(registry.isLoaded('bad.js')).to.be.false;
    expect(errors).to.deep.equal(['Error loading bad.js']);
  });

  it('clears a loaded resource and its listeners by dependency id', () => {
    const registry = new ResourceRegistry({ handleError: () => {} });
    registry.markLoaded('theme.css');
    registry.registerDependencyId('dep-1', 'theme.css');
    registry.addListener('theme.css', recordingListener().listener);
    expect(registry.isLoaded('theme.css')).to.be.true;

    registry.clearLoadedResourceById('dep-1');
    expect(registry.isLoaded('theme.css')).to.be.false;
    // The key is free again: the next listener is treated as the first.
    expect(registry.addListener('theme.css', recordingListener().listener)).to.be.true;
    // An unknown dependency id is a no-op.
    expect(() => registry.clearLoadedResourceById('nope')).to.not.throw();
  });
});
