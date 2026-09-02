// Beyond the Java suite: URIResolver has no Java test class in src/test/java or src/test-gwt/java —
// its context-root resolution comes from the base class, whose case lives in VaadinUriResolverTests — so every case here is beyond the Java suite.
import { testRegistry } from './testRegistry';
import { expect } from '@open-wc/testing';
import {
  getBaseRelativeUri,
  getCurrentLocationRelativeToBaseUri,
  URIResolver
} from '../../../../main/frontend/internal/client/URIResolver';

// resolveVaadinUri is protected on the shared base class, so the protocol cases
// go through the public URIResolver method, as its Java callers do.
const resolver = (contextRootUrl: string) =>
  new URIResolver(testRegistry({ ApplicationConfiguration: { getContextRootUrl: () => contextRootUrl } }));

describe('URIResolver', () => {
  it('resolves the context:// protocol against the context root', () => {
    expect(resolver('/ctx/').resolveVaadinUri('context://foo/bar.js')).to.equal('/ctx/foo/bar.js');
  });

  it('resolves the base:// protocol by stripping the prefix', () => {
    expect(resolver('/ctx/').resolveVaadinUri('base://foo.js')).to.equal('foo.js');
  });

  it('passes other protocols through unchanged, and null stays null', () => {
    expect(resolver('/ctx/').resolveVaadinUri('https://example.com/x.js')).to.equal('https://example.com/x.js');
    expect(resolver('/ctx/').resolveVaadinUri(null)).to.equal(null);
  });

  it('makes a uri relative to a base uri it shares, else leaves it', () => {
    expect(getBaseRelativeUri('http://h/app/', 'http://h/app/view/1')).to.equal('view/1');
    expect(getBaseRelativeUri('http://h/app/', 'http://other/x')).to.equal('http://other/x');
  });

  it('returns the current location relative to the base uri', () => {
    // The test page is served from the base uri, so the result is the page's own
    // path relative to it — never absolute, and never the whole href.
    const relative = getCurrentLocationRelativeToBaseUri();
    expect(relative).to.be.a('string');
    expect(relative.startsWith('http')).to.be.false;
    // The query string is not part of the resolved path.
    expect(`${document.baseURI.replace(/[^/]*$/, '')}${relative}`).to.equal(
      `${window.location.origin}${window.location.pathname}`
    );
  });

  it('resolves via the class against the configured context root', () => {
    const resolver = new URIResolver(testRegistry({ ApplicationConfiguration: { getContextRootUrl: () => '/ctx/' } }));
    expect(resolver.resolveVaadinUri('context://app.js')).to.equal('/ctx/app.js');
    expect(resolver.resolveVaadinUri('https://x/y')).to.equal('https://x/y');
  });
});
