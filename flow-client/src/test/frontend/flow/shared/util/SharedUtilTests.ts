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
} from '../../../../../main/frontend/internal/flow/shared/util/SharedUtil';

describe('SharedUtil', () => {
  // beyond the Java suite (PORTING.md 13.6)
  it('adds a parameter with ? to a bare URI', () => {
    expect(addGetParameter('/foo', 'v-r', 'uidl')).to.equal('/foo?v-r=uidl');
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('adds a parameter with & when a query already exists', () => {
    expect(addGetParameter('/foo?a=1', 'b', '2')).to.equal('/foo?a=1&b=2');
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('stringifies a numeric value', () => {
    expect(addGetParameter('/foo', 'v-uiId', 7)).to.equal('/foo?v-uiId=7');
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('inserts the parameter before the fragment', () => {
    expect(addGetParameter('/foo#frag', 'a', '1')).to.equal('/foo?a=1#frag');
    expect(addGetParameter('/foo?x=1#frag', 'a', '1')).to.equal('/foo?x=1&a=1#frag');
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('returns the URI unchanged for empty extra params', () => {
    expect(addGetParameters('/foo', '')).to.equal('/foo');
  });

  // beyond the Java suite (PORTING.md 13.6)
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
      'http://demo.vaadin.com:1234/foo#frag?fakeparam',
      // Jetspeed
      'http://localhost:8080/jetspeed/portal/_ns:Z3RlbXBsYXRlLXRvcDJfX3BhZ2UtdGVtcGxhdGVfX2RwLTFfX1AtMTJjNTRkYjdlYjUtMTAwMDJ8YzB8ZDF8aVVJREx8Zg__',
      // Liferay generated url
      'http://vaadin.com/directory?p_p_id=Directory_WAR_Directory&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_resource_id=UIDL&p_p_cacheability=cacheLevelPage&p_p_col_id=row-1&p_p_col_count=1'
    ];
    const withParams = [
      'http://demo.vaadin.com/?a=b&c=d',
      'https://demo.vaadin.com/?a=b&c=d',
      'http://demo.vaadin.com/foo?a=b&c=d',
      'http://demo.vaadin.com/foo?f&a=b&c=d',
      'http://demo.vaadin.com/foo?f=1&a=b&c=d',
      'http://demo.vaadin.com:1234/foo?a&a=b&c=d',
      'http://demo.vaadin.com:1234/foo?a=b&c=d#frag?fakeparam',
      'http://localhost:8080/jetspeed/portal/_ns:Z3RlbXBsYXRlLXRvcDJfX3BhZ2UtdGVtcGxhdGVfX2RwLTFfX1AtMTJjNTRkYjdlYjUtMTAwMDJ8YzB8ZDF8aVVJREx8Zg__?a=b&c=d',
      'http://vaadin.com/directory?p_p_id=Directory_WAR_Directory&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_resource_id=UIDL&p_p_cacheability=cacheLevelPage&p_p_col_id=row-1&p_p_col_count=1&a=b&c=d'
    ];
    const withParamsAndFragment = [
      'http://demo.vaadin.com/?a=b&c=d#fragment',
      'https://demo.vaadin.com/?a=b&c=d#fragment',
      'http://demo.vaadin.com/foo?a=b&c=d#fragment',
      'http://demo.vaadin.com/foo?f&a=b&c=d#fragment',
      'http://demo.vaadin.com/foo?f=1&a=b&c=d#fragment',
      'http://demo.vaadin.com:1234/foo?a&a=b&c=d#fragment',
      '', // not applicable: URI already has a fragment, so adding "#fragment" cannot add a second one
      'http://localhost:8080/jetspeed/portal/_ns:Z3RlbXBsYXRlLXRvcDJfX3BhZ2UtdGVtcGxhdGVfX2RwLTFfX1AtMTJjNTRkYjdlYjUtMTAwMDJ8YzB8ZDF8aVVJREx8Zg__?a=b&c=d#fragment',
      'http://vaadin.com/directory?p_p_id=Directory_WAR_Directory&p_p_lifecycle=2&p_p_state=normal&p_p_mode=view&p_p_resource_id=UIDL&p_p_cacheability=cacheLevelPage&p_p_col_id=row-1&p_p_col_count=1&a=b&c=d#fragment'
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

  it('trailing slash is trimmed', () => {
    expect(trimTrailingSlashes('/path/')).to.equal('/path');
  });

  it('no trailing slash for trimming', () => {
    expect(trimTrailingSlashes('/path')).to.equal('/path');
  });

  it('trailing slashes are trimmed', () => {
    expect(trimTrailingSlashes('/path///')).to.equal('/path');
  });

  it('empty string is handled', () => {
    expect(trimTrailingSlashes('')).to.equal('');
  });

  it('root slash is trimmed', () => {
    expect(trimTrailingSlashes('/')).to.equal('');
  });

  it('splits camelCase into words with casing preserved', () => {
    expect(splitCamelCase('firstName')).to.deep.equal(['first', 'Name']);
    expect(splitCamelCase('fooBar')).to.deep.equal(['foo', 'Bar']);
    expect(splitCamelCase('fBar')).to.deep.equal(['f', 'Bar']);
    expect(splitCamelCase('FBar')).to.deep.equal(['F', 'Bar']);
    expect(splitCamelCase('MYCdi')).to.deep.equal(['MY', 'Cdi']);
    expect(splitCamelCase('MyCDIUI')).to.deep.equal(['My', 'CDIUI']);
    expect(splitCamelCase('MyCDIUITwo')).to.deep.equal(['My', 'CDIUI', 'Two']);
    expect(splitCamelCase('first name')).to.deep.equal(['first', 'name']);
    expect(splitCamelCase('MyBeanContainer')).to.deep.equal(['My', 'Bean', 'Container']);
    expect(splitCamelCase('AwesomeURLFactory')).to.deep.equal(['Awesome', 'URL', 'Factory']);
    expect(splitCamelCase('SomeUriAction')).to.deep.equal(['Some', 'Uri', 'Action']);
  });

  it('converts camelCase to a human friendly format', () => {
    expect(camelCaseToHumanFriendly('firstName')).to.equal('First Name');
    expect(camelCaseToHumanFriendly('first name')).to.equal('First Name');
    expect(camelCaseToHumanFriendly('firstName2')).to.equal('First Name2');
    expect(camelCaseToHumanFriendly('first')).to.equal('First');
    expect(camelCaseToHumanFriendly('First')).to.equal('First');
    expect(camelCaseToHumanFriendly('SomeXYZAbbreviation')).to.equal('Some XYZ Abbreviation');
    expect(camelCaseToHumanFriendly('MyBeanContainer')).to.equal('My Bean Container');
    expect(camelCaseToHumanFriendly('AwesomeURLFactory')).to.equal('Awesome URL Factory');
    expect(camelCaseToHumanFriendly('SomeUriAction')).to.equal('Some Uri Action');
  });

  it('joins parts with a separator', () => {
    const s1 = 'foo-bar-baz';
    const s2 = 'foo--bar';
    expect(join(s1.split('-'), '')).to.equal('foobarbaz');
    expect(join(s1.split('-'), '!')).to.equal('foo!bar!baz');
    expect(join(s1.split('-'), '!!')).to.equal('foo!!bar!!baz');
    expect(join(s2.split('-'), '#')).to.equal('foo##bar');
    expect(join(['a', 'b', 'c'], '-')).to.equal('a-b-c');
    expect(join(['x'], '-')).to.equal('x');
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('capitalizes the first character', () => {
    // Java exercises this under the Turkish locale (capitalize must be
    // locale-independent); JavaScript upper-casing of ASCII already is.
    expect(capitalize('integer')).to.equal('Integer');
    expect(capitalize('i')).to.equal('I');
    expect(capitalize('foo')).to.equal('Foo');
    expect(capitalize('a')).to.equal('A');
    expect(capitalize('')).to.equal('');
    expect(capitalize(null)).to.equal(null);
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('lowercases the first character', () => {
    expect(firstToLower('Foo')).to.equal('foo');
    expect(firstToLower('A')).to.equal('a');
    expect(firstToLower('')).to.equal('');
    expect(firstToLower(null)).to.equal(null);
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('converts a property id to a human friendly format', () => {
    expect(propertyIdToHumanFriendly('firstName')).to.equal('First Name');
    expect(propertyIdToHumanFriendly('address.streetName')).to.equal('Street Name');
    expect(propertyIdToHumanFriendly('')).to.equal('');
  });

  it('converts dash separated strings to camelCase', () => {
    expect(dashSeparatedToCamelCase(null)).to.equal(null);
    expect(dashSeparatedToCamelCase('')).to.equal('');
    expect(dashSeparatedToCamelCase('foo')).to.equal('foo');
    expect(dashSeparatedToCamelCase('foo-bar')).to.equal('fooBar');
    expect(dashSeparatedToCamelCase('foo--bar')).to.equal('fooBar');
    expect(dashSeparatedToCamelCase('foo-bar-baz')).to.equal('fooBarBaz');
    expect(dashSeparatedToCamelCase('foo-Bar-Baz')).to.equal('fooBarBaz');
  });

  it('converts camelCase strings to dash separated', () => {
    expect(camelCaseToDashSeparated(null)).to.equal(null);
    expect(camelCaseToDashSeparated('')).to.equal('');
    expect(camelCaseToDashSeparated('foo')).to.equal('foo');
    expect(camelCaseToDashSeparated('fooBar')).to.equal('foo-bar');
    expect(camelCaseToDashSeparated('foo--bar')).to.equal('foo--bar');
    expect(camelCaseToDashSeparated('fooBarBaz')).to.equal('foo-bar-baz');
    expect(camelCaseToDashSeparated('MyBeanContainer')).to.equal('-my-bean-container');
    expect(camelCaseToDashSeparated('AwesomeURLFactory')).to.equal('-awesome-uRL-factory');
    expect(camelCaseToDashSeparated('someUriAction')).to.equal('some-uri-action');
  });

  it('converts UpperCamelCase strings to dash separated lowercase', () => {
    expect(upperCamelCaseToDashSeparatedLowerCase(null)).to.equal(null);
    expect(upperCamelCaseToDashSeparatedLowerCase('')).to.equal('');
    expect(upperCamelCaseToDashSeparatedLowerCase('foo')).to.equal('foo');
    expect(upperCamelCaseToDashSeparatedLowerCase('fooBar')).to.equal('foo-bar');
    expect(upperCamelCaseToDashSeparatedLowerCase('foo--bar')).to.equal('foo--bar');
    expect(upperCamelCaseToDashSeparatedLowerCase('fooBarBaz')).to.equal('foo-bar-baz');
    expect(upperCamelCaseToDashSeparatedLowerCase('MyBeanContainer')).to.equal('my-bean-container');
    expect(upperCamelCaseToDashSeparatedLowerCase('AwesomeURLFactory')).to.equal('awesome-url-factory');
    expect(upperCamelCaseToDashSeparatedLowerCase('someUriAction')).to.equal('some-uri-action');
  });

  // beyond the Java suite (PORTING.md 13.6)
  it('prefixes only relative urls without a protocol', () => {
    expect(prefixIfRelative('foo', '/prefix/')).to.equal('/prefix/foo');
    expect(prefixIfRelative('/foo', '/prefix/')).to.equal('/foo');
    expect(prefixIfRelative('http://demo.vaadin.com/', '/prefix/')).to.equal('http://demo.vaadin.com/');
    expect(prefixIfRelative('mailto:foo@bar.com', '/prefix/')).to.equal('mailto:foo@bar.com');
  });
});
