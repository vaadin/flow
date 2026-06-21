import { expect } from '@open-wc/testing';
import { getAbsoluteUrl, isAbsoluteUrl } from '../../main/frontend/internal/WidgetUtil';

describe('WidgetUtil', () => {
  it('isAbsoluteUrl recognizes absolute URLs', () => {
    expect(isAbsoluteUrl('http://example.com/path')).to.be.true;
    expect(isAbsoluteUrl('https://example.com')).to.be.true;
    expect(isAbsoluteUrl('//example.com/path')).to.be.true;
  });

  it('isAbsoluteUrl requires double slashes, not just a scheme', () => {
    // A scheme without // (e.g. mailto:) is not considered absolute here.
    expect(isAbsoluteUrl('mailto:foo@example.com')).to.be.false;
    expect(isAbsoluteUrl('path/to/resource')).to.be.false;
    expect(isAbsoluteUrl('/absolute/path')).to.be.false;
    expect(isAbsoluteUrl('?query=1')).to.be.false;
  });

  it('getAbsoluteUrl resolves a relative URL against the document', () => {
    const resolved = getAbsoluteUrl('foo');
    expect(isAbsoluteUrl(resolved)).to.be.true;
    expect(resolved.endsWith('/foo')).to.be.true;
  });
});
