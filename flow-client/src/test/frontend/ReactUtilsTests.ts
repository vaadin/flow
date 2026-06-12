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

import { ReactUtils } from '../../main/frontend/internal/client/ReactUtils';

describe('ReactUtils.addReadyCallback', () => {
  it('calls element.addReadyCallback with the given name and a wrapping function', () => {
    const addReadyCallback = sinon.spy();
    const runnable = sinon.spy();
    const element = Object.assign(document.createElement('div'), { addReadyCallback });

    ReactUtils.addReadyCallback(element, 'main', runnable);

    expect(addReadyCallback.calledOnce).to.be.true;
    expect(addReadyCallback.firstCall.args[0]).to.equal('main');
    addReadyCallback.firstCall.args[1]();
    expect(runnable.calledOnce).to.be.true;
  });

  it('does nothing when the element has no addReadyCallback method', () => {
    const runnable = sinon.spy();
    ReactUtils.addReadyCallback(document.createElement('div'), 'main', runnable);
    expect(runnable.called).to.be.false;
  });
});
