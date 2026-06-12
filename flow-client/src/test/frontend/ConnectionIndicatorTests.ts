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

import { ConnectionIndicator } from '../../main/frontend/internal/client/ConnectionIndicator';

const win = globalThis as unknown as { Vaadin?: any };

describe('ConnectionIndicator', () => {
  let originalVaadin: unknown;

  beforeEach(() => {
    originalVaadin = win.Vaadin;
  });

  afterEach(() => {
    win.Vaadin = originalVaadin;
  });

  it('reads and writes state via window.Vaadin.connectionState', () => {
    win.Vaadin = { connectionState: { state: 'connected' } };

    expect(ConnectionIndicator.getState()).to.equal('connected');
    ConnectionIndicator.setState('reconnecting');
    expect(win.Vaadin.connectionState.state).to.equal('reconnecting');
  });

  it('forwards loadingStarted/Finished/Failed to connectionState', () => {
    const connectionState = {
      state: 'connected',
      loadingStarted: sinon.spy(),
      loadingFinished: sinon.spy(),
      loadingFailed: sinon.spy()
    };
    win.Vaadin = { connectionState };

    ConnectionIndicator.loadingStarted();
    ConnectionIndicator.loadingFinished();
    ConnectionIndicator.loadingFailed();

    expect(connectionState.loadingStarted.calledOnce).to.be.true;
    expect(connectionState.loadingFinished.calledOnce).to.be.true;
    expect(connectionState.loadingFailed.calledOnce).to.be.true;
  });

  it('writes properties onto window.Vaadin.connectionIndicator', () => {
    const indicator: Record<string, unknown> = {};
    win.Vaadin = { connectionIndicator: indicator };

    ConnectionIndicator.setProperty('firstDelay', 250);
    expect(indicator.firstDelay).to.equal(250);
  });

  it('is a no-op when window.Vaadin is missing', () => {
    win.Vaadin = undefined;

    ConnectionIndicator.setState('x');
    ConnectionIndicator.loadingStarted();
    ConnectionIndicator.setProperty('p', 1);
    expect(ConnectionIndicator.getState()).to.be.null;
  });
});
