import { expect } from '@open-wc/testing';
import { VaadinUriResolver } from '../../../../../main/frontend/internal/flow/shared/VaadinUriResolver';

// resolveVaadinUri is protected, and the Java test reaches it through a subclass
// that supplies the context root; the same shape works here.
class NullContextVaadinUriResolver extends VaadinUriResolver {
  resolve(uri: string): string | null {
    return this.resolveVaadinUri(uri, 'http://someplace/');
  }
}

describe('VaadinUriResolver', () => {
  it('resolves the context protocol', () => {
    // Ported from testContextProtocol.
    const resolver = new NullContextVaadinUriResolver();
    expect(resolver.resolve('context://my-component.html')).to.equal('http://someplace/my-component.html');
  });
});
