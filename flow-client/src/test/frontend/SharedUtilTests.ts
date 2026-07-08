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

  it('returns the URI unchanged for null extra params', () => {
    expect(addGetParameters('/foo', null)).to.equal('/foo');
  });

  it('matches the Java SharedUtil addGetParameters URI matrix', () => {
    const uris = [
      'http://demo.vaadin.com/',
      'https://demo.vaadin.com/',
      'http://demo.vaadin.com/foo',
      'http://demo.vaadin.com/foo?f',
      'http://demo.vaadin.com/foo?f=1',
      'http://demo.vaadin.com:1234/foo?a',
      'http://demo.vaadin.com:1234/foo#frag?fakeparam'
    ];
    const withParams = [
      'http://demo.vaadin.com/?a=b&c=d',
      'https://demo.vaadin.com/?a=b&c=d',
      'http://demo.vaadin.com/foo?a=b&c=d',
      'http://demo.vaadin.com/foo?f&a=b&c=d',
      'http://demo.vaadin.com/foo?f=1&a=b&c=d',
      'http://demo.vaadin.com:1234/foo?a&a=b&c=d',
      'http://demo.vaadin.com:1234/foo?a=b&c=d#frag?fakeparam'
    ];
    const withParamsAndFragment = [
      'http://demo.vaadin.com/?a=b&c=d#fragment',
      'https://demo.vaadin.com/?a=b&c=d#fragment',
      'http://demo.vaadin.com/foo?a=b&c=d#fragment',
      'http://demo.vaadin.com/foo?f&a=b&c=d#fragment',
      'http://demo.vaadin.com/foo?f=1&a=b&c=d#fragment',
      'http://demo.vaadin.com:1234/foo?a&a=b&c=d#fragment',
      ''
    ];

    uris.forEach((uri, i) => {
      expect(addGetParameters(uri, '')).to.equal(uri);
      expect(addGetParameters(uri, 'a=b&c=d')).to.equal(withParams[i]);

      if (withParamsAndFragment[i].length > 0) {
        expect(addGetParameters(`${uri}#fragment`, 'a=b&c=d')).to.equal(withParamsAndFragment[i]);
        expect(addGetParameters(`${uri}#`, 'a=b&c=d')).to.equal(withParamsAndFragment[i].replace('#fragment', '#'));
      }
    });
  });
});
