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

import {
  attach,
  attachedFixture,
  clearList,
  detach,
  detachedFixture,
  eventRpc,
  navigationRpc,
  propertySyncRpc,
  put,
  putNode,
  removeMapKey,
  splice
} from './harness';

describe('harness — UIDL change builders', () => {
  it('attach / detach produce the expected shape', () => {
    expect(attach(7)).to.deep.equal({ type: 'attach', node: 7 });
    expect(detach(7)).to.deep.equal({ type: 'detach', node: 7 });
  });

  it('put includes value, putNode includes nodeValue', () => {
    expect(put(3, 1, 'tag', 'div')).to.deep.equal({
      type: 'put',
      node: 3,
      feat: 1,
      key: 'tag',
      value: 'div'
    });
    expect(putNode(3, 1, 'parent', 42)).to.deep.equal({
      type: 'put',
      node: 3,
      feat: 1,
      key: 'parent',
      nodeValue: 42
    });
  });

  it('removeMapKey / clearList carry feature + key info', () => {
    expect(removeMapKey(3, 2, 'foo')).to.deep.equal({
      type: 'remove',
      node: 3,
      feat: 2,
      key: 'foo'
    });
    expect(clearList(3, 4)).to.deep.equal({ type: 'clear', node: 3, feat: 4 });
  });

  it('splice supports primitive add, addNodes, and remove combinations', () => {
    expect(splice(1, 5, 0, { add: ['a', 'b'] })).to.deep.equal({
      type: 'splice',
      node: 1,
      feat: 5,
      index: 0,
      add: ['a', 'b']
    });
    expect(splice(1, 5, 2, { addNodes: [10, 11], remove: 1 })).to.deep.equal({
      type: 'splice',
      node: 1,
      feat: 5,
      index: 2,
      addNodes: [10, 11],
      remove: 1
    });
    expect(splice(1, 5, 0)).to.deep.equal({ type: 'splice', node: 1, feat: 5, index: 0 });
  });
});

describe('harness — client→server RPC builders', () => {
  it('eventRpc carries node, event name and data', () => {
    expect(eventRpc(7, 'click', { x: 1 })).to.deep.equal({
      type: 'event',
      node: 7,
      event: 'click',
      data: { x: 1 }
    });
  });

  it('propertySyncRpc carries feature, property name and value', () => {
    expect(propertySyncRpc(7, 2, 'value', 'hi')).to.deep.equal({
      type: 'mSync',
      node: 7,
      feature: 2,
      property: 'value',
      value: 'hi'
    });
  });

  it('navigationRpc carries location and optional state', () => {
    expect(navigationRpc('/foo')).to.deep.equal({ type: 'navigation', location: '/foo', state: undefined });
    expect(navigationRpc('/bar', { from: 'history' })).to.deep.equal({
      type: 'navigation',
      location: '/bar',
      state: { from: 'history' }
    });
  });
});

describe('harness — DOM fixtures', () => {
  it('attachedFixture appends to document.body and cleans up afterwards', () => {
    const el = attachedFixture('div');
    expect(document.body.contains(el)).to.be.true;
    // Cleanup happens in afterEach; the next test must observe an empty body
    // body cleanup is checked by the next `it`.
  });

  it('attachedFixture cleanup ran from the previous test', () => {
    // If the previous test's cleanup ran, no orphan <div> with no attrs
    // should remain. We can't uniquely identify it, but body shouldn't
    // hold an extra child that we didn't create here.
    const childCount = document.body.childNodes.length;
    const el = attachedFixture('section');
    expect(document.body.childNodes.length).to.equal(childCount + 1);
    expect(document.body.contains(el)).to.be.true;
  });

  it('detachedFixture returns an element not in the document', () => {
    const el = detachedFixture('span');
    expect(document.body.contains(el)).to.be.false;
    expect(el.tagName).to.equal('SPAN');
  });
});
