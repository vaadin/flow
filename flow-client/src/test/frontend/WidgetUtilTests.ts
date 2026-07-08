import { expect } from '@open-wc/testing';
import {
  createJsonObject,
  createJsonObjectWithoutPrototype,
  deleteJsProperty,
  equalsInJS,
  getAbsoluteUrl,
  getJsProperty,
  getKeys,
  hasJsProperty,
  hasOwnJsProperty,
  isAbsoluteUrl,
  isTrueish,
  isUndefined,
  setJsProperty,
  stringify,
  toPrettyJson,
  updateAttribute
} from '../../main/frontend/internal/WidgetUtil';

describe('WidgetUtil', () => {
  it('updateAttribute sets a value and removes it on null', () => {
    const element = document.createElement('div');
    updateAttribute(element, 'title', 'hello');
    expect(element.getAttribute('title')).to.equal('hello');
    updateAttribute(element, 'title', null);
    expect(element.hasAttribute('title')).to.be.false;
  });

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

  it('get/set/deleteJsProperty round-trip a property', () => {
    const obj: Record<string, unknown> = {};
    setJsProperty(obj, 'x', 42);
    expect(getJsProperty(obj, 'x')).to.equal(42);
    expect(hasOwnJsProperty(obj, 'x')).to.be.true;
    deleteJsProperty(obj, 'x');
    expect(getJsProperty(obj, 'x')).to.equal(undefined);
    expect(hasOwnJsProperty(obj, 'x')).to.be.false;
  });

  it('hasOwnJsProperty ignores inherited properties, hasJsProperty does not', () => {
    const obj: Record<string, unknown> = Object.create({ inherited: 1 });
    obj.own = 2;
    expect(hasOwnJsProperty(obj, 'own')).to.be.true;
    expect(hasOwnJsProperty(obj, 'inherited')).to.be.false;
    expect(hasJsProperty(obj, 'inherited')).to.be.true;
    expect(hasJsProperty(obj, 'missing')).to.be.false;
  });

  it('isUndefined distinguishes undefined from null', () => {
    expect(isUndefined(undefined)).to.be.true;
    expect(isUndefined(null)).to.be.false;
    expect(isUndefined(0)).to.be.false;
  });

  it('isTrueish follows JavaScript truthiness', () => {
    expect(isTrueish(1)).to.be.true;
    expect(isTrueish('x')).to.be.true;
    expect(isTrueish(0)).to.be.false;
    expect(isTrueish('')).to.be.false;
    expect(isTrueish(null)).to.be.false;
  });

  it('getKeys returns own enumerable keys', () => {
    expect(getKeys({ a: 1, b: 2 })).to.eql(['a', 'b']);
  });

  it('equalsInJS uses loose equality', () => {
    expect(equalsInJS(0, '')).to.be.true;
    expect(equalsInJS('1', 1)).to.be.true;
    expect(equalsInJS(null, undefined)).to.be.true;
    expect(equalsInJS(1, 2)).to.be.false;
  });

  it('createJsonObject has a prototype, createJsonObjectWithoutPrototype does not', () => {
    expect(Object.getPrototypeOf(createJsonObject())).to.equal(Object.prototype);
    expect(Object.getPrototypeOf(createJsonObjectWithoutPrototype())).to.equal(null);
  });

  it('toPrettyJson indents and skips the $H hashCode field', () => {
    const pretty = toPrettyJson({ a: 1, $H: 99 });
    expect(pretty).to.contain('\n');
    expect(pretty).to.not.contain('$H');
    expect(JSON.parse(pretty)).to.eql({ a: 1 });
  });

  it('stringify serializes a plain object', () => {
    expect(stringify({ a: 1, b: 'x' })).to.equal('{"a":1,"b":"x"}');
  });

  it('stringify throws when the object contains a DOM node', () => {
    expect(() => stringify({ node: document.createElement('div') })).to.throw();
  });
});
