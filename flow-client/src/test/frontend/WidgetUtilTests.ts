/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import { expect } from '@open-wc/testing';

import { WidgetUtil } from '../../main/frontend/internal/client/WidgetUtil';

describe('WidgetUtil', () => {
  describe('isAbsoluteUrl', () => {
    it('accepts http(s) and protocol-relative URLs', () => {
      expect(WidgetUtil.isAbsoluteUrl('https://example.com')).to.be.true;
      expect(WidgetUtil.isAbsoluteUrl('http://example.com')).to.be.true;
      expect(WidgetUtil.isAbsoluteUrl('//example.com')).to.be.true;
    });

    it('rejects relative URLs and schemes without //', () => {
      expect(WidgetUtil.isAbsoluteUrl('/foo')).to.be.false;
      expect(WidgetUtil.isAbsoluteUrl('foo/bar')).to.be.false;
      // matches the original regex, which requires `//` after the scheme
      expect(WidgetUtil.isAbsoluteUrl('mailto:foo@bar')).to.be.false;
    });
  });

  describe('JS property accessors', () => {
    it('round-trips set/get/has/delete on a plain object', () => {
      const obj: Record<string, unknown> = {};
      WidgetUtil.setJsProperty(obj, 'a', 1);
      expect(WidgetUtil.getJsProperty(obj, 'a')).to.equal(1);
      expect(WidgetUtil.hasOwnJsProperty(obj, 'a')).to.be.true;
      expect(WidgetUtil.hasJsProperty(obj, 'a')).to.be.true;
      WidgetUtil.deleteJsProperty(obj, 'a');
      expect(WidgetUtil.hasOwnJsProperty(obj, 'a')).to.be.false;
    });

    it('distinguishes own from inherited properties', () => {
      const obj = Object.create({ inherited: 42 });
      expect(WidgetUtil.hasOwnJsProperty(obj, 'inherited')).to.be.false;
      expect(WidgetUtil.hasJsProperty(obj, 'inherited')).to.be.true;
    });
  });

  describe('JSON helpers', () => {
    it('creates plain and prototypeless objects', () => {
      expect(Object.getPrototypeOf(WidgetUtil.createJsonObject())).to.equal(Object.prototype);
      expect(Object.getPrototypeOf(WidgetUtil.createJsonObjectWithoutPrototype())).to.equal(null);
    });

    it('formats a JSON value with indentation and skips the $H hashCode key', () => {
      const formatted = WidgetUtil.toPrettyJsonJsni({ a: 1, $H: 99 });
      expect(formatted).to.equal(`{\n    "a": 1\n}`);
    });

    it('stringify refuses to serialize DOM nodes', () => {
      expect(() => WidgetUtil.stringify({ node: document.createElement('div') })).to.throw();
    });
  });

  describe('predicate helpers', () => {
    it('isTrueish mirrors JS !! semantics', () => {
      expect(WidgetUtil.isTrueish('')).to.be.false;
      expect(WidgetUtil.isTrueish(0)).to.be.false;
      expect(WidgetUtil.isTrueish(null)).to.be.false;
      expect(WidgetUtil.isTrueish(undefined)).to.be.false;
      expect(WidgetUtil.isTrueish('x')).to.be.true;
      expect(WidgetUtil.isTrueish(1)).to.be.true;
    });

    it('isUndefined distinguishes undefined from null', () => {
      expect(WidgetUtil.isUndefined(undefined)).to.be.true;
      expect(WidgetUtil.isUndefined(null)).to.be.false;
    });

    it('equalsInJS uses loose equality', () => {
      expect(WidgetUtil.equalsInJS('1', 1)).to.be.true;
      expect(WidgetUtil.equalsInJS(0, '')).to.be.true;
      expect(WidgetUtil.equalsInJS('a', 'b')).to.be.false;
    });

    it('getKeys mirrors Object.keys', () => {
      expect(WidgetUtil.getKeys({ a: 1, b: 2 })).to.deep.equal(['a', 'b']);
    });
  });
});
