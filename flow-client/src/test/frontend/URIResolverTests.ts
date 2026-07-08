import { expect } from '@open-wc/testing';
import {
  getBaseRelativeUri,
  getCurrentLocationRelativeToBaseUri,
  resolveVaadinUri,
  URIResolver
} from '../../main/frontend/internal/URIResolver';

describe('URIResolver', () => {
  it('resolves the context:// protocol against the context root', () => {
    expect(resolveVaadinUri('context://foo/bar.js', '/ctx/')).to.equal('/ctx/foo/bar.js');
  });

  it('resolves the base:// protocol by stripping the prefix', () => {
    expect(resolveVaadinUri('base://foo.js', '/ctx/')).to.equal('foo.js');
  });

  it('passes other protocols through unchanged, and null stays null', () => {
    expect(resolveVaadinUri('https://example.com/x.js', '/ctx/')).to.equal('https://example.com/x.js');
    expect(resolveVaadinUri(null, '/ctx/')).to.equal(null);
  });

  it('makes a uri relative to a base uri it shares, else leaves it', () => {
    expect(getBaseRelativeUri('http://h/app/', 'http://h/app/view/1')).to.equal('view/1');
    expect(getBaseRelativeUri('http://h/app/', 'http://other/x')).to.equal('http://other/x');
  });

  it('returns the current location relative to the base uri as a string', () => {
    expect(getCurrentLocationRelativeToBaseUri()).to.be.a('string');
  });

  it('resolves via the class against the configured context root', () => {
    const resolver = new URIResolver({ getApplicationConfiguration: () => ({ getContextRootUrl: () => '/ctx/' }) });
    expect(resolver.resolveVaadinUri('context://app.js')).to.equal('/ctx/app.js');
    expect(resolver.resolveVaadinUri('https://x/y')).to.equal('https://x/y');
  });
});
