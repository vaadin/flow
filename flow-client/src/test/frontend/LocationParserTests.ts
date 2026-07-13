import { expect } from '@open-wc/testing';
import { getParameter } from '../../main/frontend/internal/LocationParser';

describe('LocationParser', () => {
  it('parses parameters from a search string', () => {
    expect(getParameter('?', 'foo')).to.equal(null);
    expect(getParameter('?bar', 'foo')).to.equal(null);
    expect(getParameter('?foo', 'foo')).to.equal('');
    expect(getParameter('?foo=', 'foo')).to.equal('');
    expect(getParameter('?foo=bar', 'foo')).to.equal('bar');
    expect(getParameter('?foo=bar&', 'foo')).to.equal('bar');
    expect(getParameter('?foo&bar', 'foo')).to.equal('');
    expect(getParameter('?bar&foo', 'foo')).to.equal('');
    expect(getParameter('?bar&foo=', 'foo')).to.equal('');
    expect(getParameter('?bar&foo=a', 'foo')).to.equal('a');
  });

  it('keeps everything after the first = as the value', () => {
    expect(getParameter('?foo=a=b', 'foo')).to.equal('a=b');
  });
});
