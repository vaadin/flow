import { expect } from '@open-wc/testing';
import { addGetParameter, addGetParameters } from '../../main/frontend/internal/SharedUtil';

describe('SharedUtil', () => {
  it('adds a parameter with ? to a bare URI', () => {
    expect(addGetParameter('/foo', 'v-r', 'uidl')).to.equal('/foo?v-r=uidl');
  });

  it('adds a parameter with & when a query already exists', () => {
    expect(addGetParameter('/foo?a=1', 'b', '2')).to.equal('/foo?a=1&b=2');
  });

  it('stringifies a numeric value', () => {
    expect(addGetParameter('/foo', 'v-uiId', 7)).to.equal('/foo?v-uiId=7');
  });

  it('inserts the parameter before the fragment', () => {
    expect(addGetParameter('/foo#frag', 'a', '1')).to.equal('/foo?a=1#frag');
    expect(addGetParameter('/foo?x=1#frag', 'a', '1')).to.equal('/foo?x=1&a=1#frag');
  });

  it('returns the URI unchanged for empty extra params', () => {
    expect(addGetParameters('/foo', '')).to.equal('/foo');
  });
});
