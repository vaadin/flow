import { expect } from '@open-wc/testing';
import {
  addGetParameter,
  addGetParameters,
  camelCaseToDashSeparated,
  camelCaseToHumanFriendly,
  capitalize,
  dashSeparatedToCamelCase,
  firstToLower,
  join,
  prefixIfRelative,
  propertyIdToHumanFriendly,
  splitCamelCase,
  trimTrailingSlashes,
  upperCamelCaseToDashSeparatedLowerCase
} from '../../main/frontend/internal/SharedUtil';

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
      '' // not applicable: URI already has a fragment, so adding "#fragment" cannot add a second one
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

  it('trims trailing slashes', () => {
    expect(trimTrailingSlashes('foo')).to.equal('foo');
    expect(trimTrailingSlashes('foo/')).to.equal('foo');
    expect(trimTrailingSlashes('foo///')).to.equal('foo');
    expect(trimTrailingSlashes('/')).to.equal('');
  });

  it('splits camelCase into words with casing preserved', () => {
    expect(splitCamelCase('MyBeanContainer')).to.deep.equal(['My', 'Bean', 'Container']);
    expect(splitCamelCase('AwesomeURLFactory')).to.deep.equal(['Awesome', 'URL', 'Factory']);
    expect(splitCamelCase('SomeUriAction')).to.deep.equal(['Some', 'Uri', 'Action']);
  });

  it('converts camelCase to a human friendly format', () => {
    expect(camelCaseToHumanFriendly('MyBeanContainer')).to.equal('My Bean Container');
    expect(camelCaseToHumanFriendly('AwesomeURLFactory')).to.equal('Awesome URL Factory');
    expect(camelCaseToHumanFriendly('SomeUriAction')).to.equal('Some Uri Action');
  });

  it('joins parts with a separator', () => {
    expect(join(['a', 'b', 'c'], '-')).to.equal('a-b-c');
    expect(join(['x'], '-')).to.equal('x');
  });

  it('capitalizes the first character', () => {
    expect(capitalize('foo')).to.equal('Foo');
    expect(capitalize('a')).to.equal('A');
    expect(capitalize('')).to.equal('');
    expect(capitalize(null)).to.equal(null);
  });

  it('lowercases the first character', () => {
    expect(firstToLower('Foo')).to.equal('foo');
    expect(firstToLower('A')).to.equal('a');
    expect(firstToLower('')).to.equal('');
    expect(firstToLower(null)).to.equal(null);
  });

  it('converts a property id to a human friendly format', () => {
    expect(propertyIdToHumanFriendly('firstName')).to.equal('First Name');
    expect(propertyIdToHumanFriendly('address.streetName')).to.equal('Street Name');
    expect(propertyIdToHumanFriendly('')).to.equal('');
  });

  it('converts dash separated strings to camelCase', () => {
    expect(dashSeparatedToCamelCase('foo')).to.equal('foo');
    expect(dashSeparatedToCamelCase('foo-bar')).to.equal('fooBar');
    expect(dashSeparatedToCamelCase('foo--bar')).to.equal('fooBar');
    expect(dashSeparatedToCamelCase(null)).to.equal(null);
  });

  it('converts camelCase strings to dash separated', () => {
    expect(camelCaseToDashSeparated('foo')).to.equal('foo');
    expect(camelCaseToDashSeparated('fooBar')).to.equal('foo-bar');
    expect(camelCaseToDashSeparated('MyBeanContainer')).to.equal('-my-bean-container');
    expect(camelCaseToDashSeparated('AwesomeURLFactory')).to.equal('-awesome-uRL-factory');
    expect(camelCaseToDashSeparated('someUriAction')).to.equal('some-uri-action');
    expect(camelCaseToDashSeparated(null)).to.equal(null);
  });

  it('converts UpperCamelCase strings to dash separated lowercase', () => {
    expect(upperCamelCaseToDashSeparatedLowerCase('foo')).to.equal('foo');
    expect(upperCamelCaseToDashSeparatedLowerCase('fooBar')).to.equal('foo-bar');
    expect(upperCamelCaseToDashSeparatedLowerCase('MyBeanContainer')).to.equal('my-bean-container');
    expect(upperCamelCaseToDashSeparatedLowerCase('AwesomeURLFactory')).to.equal('awesome-url-factory');
    expect(upperCamelCaseToDashSeparatedLowerCase('someUriAction')).to.equal('some-uri-action');
    expect(upperCamelCaseToDashSeparatedLowerCase(null)).to.equal(null);
  });

  it('prefixes only relative urls without a protocol', () => {
    expect(prefixIfRelative('foo', '/prefix/')).to.equal('/prefix/foo');
    expect(prefixIfRelative('/foo', '/prefix/')).to.equal('/foo');
    expect(prefixIfRelative('http://demo.vaadin.com/', '/prefix/')).to.equal('http://demo.vaadin.com/');
    expect(prefixIfRelative('mailto:foo@bar.com', '/prefix/')).to.equal('mailto:foo@bar.com');
  });
});
