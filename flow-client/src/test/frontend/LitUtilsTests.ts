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

import { LitUtils } from '../../main/frontend/internal/client/LitUtils';

function fakeLitElement(updateComplete: Promise<unknown>): any {
  return {
    update() {},
    shouldUpdate() {
      return true;
    },
    firstUpdated() {},
    updateComplete
  };
}

describe('LitUtils', () => {
  describe('isLitElement', () => {
    it('returns true for an element with all four Lit hooks', () => {
      expect(LitUtils.isLitElement(fakeLitElement(Promise.resolve()))).to.be.true;
    });

    it('returns false when updateComplete is not a Promise', () => {
      const elem = fakeLitElement(Promise.resolve());
      elem.updateComplete = true;
      expect(LitUtils.isLitElement(elem)).to.be.false;
    });

    it('returns false when any hook is missing', () => {
      const elem = fakeLitElement(Promise.resolve());
      delete elem.firstUpdated;
      expect(LitUtils.isLitElement(elem)).to.be.false;
    });

    it('returns false for null and plain HTMLElement', () => {
      expect(LitUtils.isLitElement(null)).to.be.false;
      expect(LitUtils.isLitElement(document.createElement('div'))).to.be.false;
    });
  });

  describe('whenRendered', () => {
    it('invokes the callback after updateComplete resolves', async () => {
      const callback = sinon.spy();
      const elem = fakeLitElement(Promise.resolve());

      LitUtils.whenRendered(elem, callback);
      await elem.updateComplete;
      // Wait one microtask so the chained .then() runs.
      await Promise.resolve();

      expect(callback.calledOnce).to.be.true;
    });

    it('does nothing when the element has no updateComplete', () => {
      const callback = sinon.spy();
      LitUtils.whenRendered(document.createElement('div'), callback);
      expect(callback.called).to.be.false;
    });
  });
});
