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
import sinon from 'sinon';

import { PolymerUtils } from '../../main/frontend/internal/client/PolymerUtils';

describe('PolymerUtils', () => {
  describe('setListValueByIndex', () => {
    it('calls set with the index-suffixed path', () => {
      const set = sinon.spy();
      PolymerUtils.setListValueByIndex({ set } as unknown as Element, 'items', 3, 'v');
      expect(set.calledOnceWith('items.3', 'v')).to.be.true;
    });
  });

  describe('splice', () => {
    it('passes (path, start, deleteCount, ...items) to elem.splice', () => {
      const splice = sinon.spy();
      PolymerUtils.splice({ splice } as unknown as Element, 'items', 1, 0, ['a', 'b']);
      expect(splice.firstCall.args).to.deep.equal(['items', 1, 0, 'a', 'b']);
    });
  });

  describe('storeNodeId', () => {
    it('writes nodeId into the polymer property when missing', () => {
      const prop: any = {};
      const node = { get: () => prop } as unknown as Node;
      PolymerUtils.storeNodeId(node, 42, 'p');
      expect(prop.nodeId).to.equal(42);
    });

    it('does not overwrite an existing nodeId', () => {
      const prop = { nodeId: 1 };
      const node = { get: () => prop } as unknown as Node;
      PolymerUtils.storeNodeId(node, 99, 'p');
      expect(prop.nodeId).to.equal(1);
    });
  });

  describe('isPolymerElement', () => {
    it('detects Polymer v3 elements by polymerElementVersion on constructor', () => {
      const FooCtor = function Foo(this: any) {
        /* no-op */
      } as any;
      FooCtor.polymerElementVersion = '3.x';
      const el = new FooCtor() as unknown as Element;
      expect(PolymerUtils.isPolymerElement(el)).to.be.true;
    });

    it('returns false for a plain HTMLElement', () => {
      expect(PolymerUtils.isPolymerElement(document.createElement('div'))).to.be.false;
    });
  });

  describe('isReady', () => {
    it('returns true when shadowRootParent.$ exists', () => {
      expect(PolymerUtils.isReady({ $: {} } as unknown as Node)).to.be.true;
      expect(PolymerUtils.isReady({} as unknown as Node)).to.be.false;
    });
  });

  describe('getDomElementById', () => {
    it('reads from shadowRootParent.$', () => {
      const target = document.createElement('span');
      expect(PolymerUtils.getDomElementById({ $: { foo: target } } as unknown as Node, 'foo')).to.equal(target);
    });
  });

  describe('isInShadowRoot', () => {
    it('returns true when the element is inside a shadow root', () => {
      const host = document.createElement('div');
      document.body.appendChild(host);
      const root = host.attachShadow({ mode: 'open' });
      const child = document.createElement('span');
      root.appendChild(child);
      try {
        expect(PolymerUtils.isInShadowRoot(child)).to.be.true;
      } finally {
        host.remove();
      }
    });

    it('returns false for an element in the light DOM', () => {
      const child = document.createElement('span');
      document.body.appendChild(child);
      try {
        expect(PolymerUtils.isInShadowRoot(child)).to.be.false;
      } finally {
        child.remove();
      }
    });
  });
});
