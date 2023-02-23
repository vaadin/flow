import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { MetadataRegistry } from './registry';
import { ComponentReference } from '../../component-util';

describe('metadata-registry', () => {
  let moduleLoaderSpy: sinon.SinonSpy;
  let registry: MetadataRegistry;

  function mockComponentReference(tagName?: string): ComponentReference {
    return {
      nodeId: 1,
      uiId: 1,
      element: tagName ? ({ localName: tagName } as any) : null
    };
  }

  beforeEach(() => {
    // Setup default registry with mock loader that returns a mock module
    moduleLoaderSpy = sinon.spy(() => Promise.resolve({ default: {} }));
    registry = new MetadataRegistry(moduleLoaderSpy);
  });

  it('should not load metadata for unknown elements', async () => {
    // No element
    let componentRef = mockComponentReference();
    let metadata = await registry.getMetadata(componentRef);

    expect(metadata).to.be.null;
    expect(moduleLoaderSpy.called).to.be.false;

    // Unknown element
    componentRef = mockComponentReference('unknown-element');
    metadata = await registry.getMetadata(componentRef);

    expect(metadata).to.be.null;
    expect(moduleLoaderSpy.called).to.be.false;
  });

  it('should return null for unknown Vaadin component', async () => {
    // Setup registry with mock loader that fails to load module
    moduleLoaderSpy = sinon.spy(() => Promise.reject('could not find module'));
    registry = new MetadataRegistry(moduleLoaderSpy);

    const componentRef = mockComponentReference('vaadin-unknown-element');
    const metadata = await registry.getMetadata(componentRef);

    expect(metadata).to.be.null;
    expect(moduleLoaderSpy.calledWith('vaadin-unknown-element')).to.be.true;
  });

  it('should dynamically load metadata for known Vaadin component', async () => {
    // Setup registry with default metadata loader using dynamic imports
    registry = new MetadataRegistry();

    const componentRef = mockComponentReference('vaadin-button');
    const metadata = await registry.getMetadata(componentRef);

    expect(metadata).to.not.be.null;
    expect(metadata!.tagName).to.equal('vaadin-button');
    expect(metadata!.displayName).to.equal('Button');
    expect(metadata!.parts).to.be.instanceof(Array);
  });

  it('should cache metadata', async () => {
    const componentRef = mockComponentReference('vaadin-button');

    await registry.getMetadata(componentRef);
    await registry.getMetadata(componentRef);
    await registry.getMetadata(componentRef);

    expect(moduleLoaderSpy.calledOnce).to.be.true;
  });
});
