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
import { assert } from '@open-wc/testing';
import '../../main/frontend/ConnectionIndicator';
import type { ConnectionIndicator } from '../../main/frontend/ConnectionIndicator';
import { ConnectionState, ConnectionStateStore } from '../../main/frontend/ConnectionState';

const $wnd = window as any;

async function sleep(ms: number) {
  await new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

describe('ConnectionIndicator', () => {
  let connectionIndicator: ConnectionIndicator;

  describe('properties', () => {
    beforeEach(async () => {
      connectionIndicator = document.createElement('vaadin-connection-indicator');
      document.body.prepend(connectionIndicator);
      await connectionIndicator.updateComplete;
    });

    afterEach(() => {
      connectionIndicator.remove();
    });

    it('should have default property values', () => {
      // Configuration
      assert.isTrue(connectionIndicator.applyDefaultTheme);
      assert.equal(connectionIndicator.firstDelay, 450);
      assert.equal(connectionIndicator.secondDelay, 1500);
      assert.equal(connectionIndicator.thirdDelay, 5000);
      assert.equal(connectionIndicator.expandedDuration, 2000);

      // Strings
      assert.match(connectionIndicator.onlineText, /online/iu);
      assert.match(connectionIndicator.offlineText, /connection lost/iu);
      assert.match(connectionIndicator.reconnectingText, /reconnect/iu);

      // Component state
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
    });

    it('should apply default theme', () => {
      const style = document.querySelector('#css-loading-indicator');
      assert.isNotNull(style);
      const functionalStyle = document.querySelector('#css-loading-indicator-functional');
      assert.isNotNull(functionalStyle);
      const headChildNodes = Array.from(document.head.childNodes);
      const functionalStyleOrder = headChildNodes.indexOf(functionalStyle);
      const styleOrder = headChildNodes.indexOf(style);
      assert.isBelow(
        functionalStyleOrder,
        styleOrder,
        'functional indicator style must come before theme style in document.head'
      );
    });

    it('should remove css if default theme not applied', () => {
      connectionIndicator.applyDefaultTheme = false;
      const style = document.querySelector('#css-loading-indicator');
      assert.isNull(style);
      const functionalStyle = document.querySelector('#css-loading-indicator-functional');
      assert.isNotNull(functionalStyle);
    });

    it('should remove css when removed from dom', () => {
      connectionIndicator.remove();
      const style = document.querySelector('#css-loading-indicator');
      assert.isNull(style);
      const functionalStyle = document.querySelector('#css-loading-indicator-functional');
      assert.isNull(functionalStyle);

      // Add back to prevent errors in afterEach
      document.body.prepend(connectionIndicator);
    });

    it('should set popover attribute when connected', () => {
      assert.equal(connectionIndicator.getAttribute('popover'), 'manual');
    });

    it('should set popover styles when connected', () => {
      const connectionIndicatorStyle = window.getComputedStyle(connectionIndicator);
      assert.equal(connectionIndicatorStyle.display, 'contents');
      assert.equal(connectionIndicatorStyle.width, 'auto');
      assert.equal(connectionIndicatorStyle.height, 'auto');
      assert.equal(Number.parseFloat(connectionIndicatorStyle.top), 0);
      assert.equal(Number.parseFloat(connectionIndicatorStyle.right), 0);
      assert.equal(connectionIndicatorStyle.bottom, 'auto');
      assert.equal(Number.parseFloat(connectionIndicatorStyle.left), 0);
      assert.equal(Number.parseFloat(connectionIndicatorStyle.margin), 0);
      assert.equal(Number.parseFloat(connectionIndicatorStyle.padding), 0);
      assert.equal(connectionIndicatorStyle.backgroundColor, 'rgba(0, 0, 0, 0)');
      assert.equal(connectionIndicatorStyle.borderStyle, 'none');
    });

    it('should be invisible when default theme is not applied', () => {
      connectionIndicator.applyDefaultTheme = false;
      const loadingIndicator = connectionIndicator.querySelector('.v-loading-indicator')!;
      const loadingIndicatorStyle = window.getComputedStyle(loadingIndicator);
      assert.equal(Number.parseFloat(loadingIndicatorStyle.opacity), 0);
      assert.equal(loadingIndicatorStyle.pointerEvents, 'none');
      const messageBar = connectionIndicator.querySelector('.v-status-message')!;
      const messageBarStyle = window.getComputedStyle(messageBar);
      assert.equal(Number.parseFloat(messageBarStyle.opacity), 0);
      assert.equal(messageBarStyle.pointerEvents, 'none');
    });

    it('should bypass pointer events by default', () => {
      const loadingIndicator = connectionIndicator.querySelector('.v-loading-indicator')!;
      const loadingIndicatorStyle = window.getComputedStyle(loadingIndicator);
      assert.equal(loadingIndicatorStyle.pointerEvents, 'none');
      const messageBar = connectionIndicator.querySelector('.v-status-message')!;
      const messageBarStyle = window.getComputedStyle(messageBar);
      assert.equal(messageBarStyle.pointerEvents, 'none');
    });

    it('should have border-box sizing', () => {
      const messageBar = connectionIndicator.querySelector('.v-status-message')!;
      assert.equal(window.getComputedStyle(messageBar).boxSizing, 'border-box');
    });
  });

  describe('with state store', () => {
    let connectionStateStore: ConnectionStateStore;
    let message: HTMLSpanElement;

    async function setupIndicator() {
      connectionIndicator = document.createElement('vaadin-connection-indicator');
      connectionIndicator.expandedDuration = 10;
      document.body.prepend(connectionIndicator);
      await connectionIndicator.updateComplete;
      message = connectionIndicator.querySelector('.v-status-message > span')!;
    }

    function isPopoverOpen() {
      return document.querySelector('vaadin-connection-indicator:popover-open') !== null;
    }

    beforeEach(() => {
      connectionStateStore = new ConnectionStateStore(ConnectionState.CONNECTED);
      $wnd.Vaadin = { connectionState: connectionStateStore };
    });

    afterEach(() => {
      delete $wnd.Vaadin;
      connectionIndicator.remove();
    });

    it('initial state: connected', async () => {
      await setupIndicator();

      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
    });

    it('initial state: loading', async () => {
      connectionStateStore.state = ConnectionState.LOADING;
      await setupIndicator();

      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isTrue(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
      assert.isTrue(isPopoverOpen());
    });

    it('initial state: reconnecting', async () => {
      connectionStateStore.state = ConnectionState.RECONNECTING;

      await setupIndicator();

      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isTrue(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.reconnectingText);
      assert.isTrue(isPopoverOpen());
    });

    it('initial state: connection lost', async () => {
      connectionStateStore.state = ConnectionState.CONNECTION_LOST;

      await setupIndicator();

      assert.isTrue(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.offlineText);
      assert.isTrue(isPopoverOpen());
    });

    it('should react on store state change', async () => {
      await setupIndicator();

      const messageBar = connectionIndicator.querySelector('.v-status-message')!;

      connectionStateStore.state = ConnectionState.LOADING;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
      assert.isTrue(connectionIndicator.hasAttribute('loading'));
      // Loading should not cause expanded message
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      assert.equal(window.getComputedStyle(messageBar).pointerEvents, 'none');

      connectionStateStore.state = ConnectionState.CONNECTED;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
      // Message did not change from before loading, should not cause expanded
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.isFalse(isPopoverOpen());
      assert.equal(window.getComputedStyle(messageBar).pointerEvents, 'none');

      connectionStateStore.state = ConnectionState.RECONNECTING;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isTrue(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.reconnectingText);
      // Message did change, should cause expanded
      assert.isTrue(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      await sleep(20);
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      assert.equal(window.getComputedStyle(messageBar).pointerEvents, 'auto');

      connectionStateStore.state = ConnectionState.CONNECTION_LOST;
      await connectionIndicator.updateComplete;
      assert.isTrue(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.offlineText);
      // Message did change, should cause expanded
      assert.isTrue(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      await sleep(20);
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      assert.equal(window.getComputedStyle(messageBar).pointerEvents, 'auto');

      connectionStateStore.state = ConnectionState.LOADING;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
      assert.isTrue(connectionIndicator.hasAttribute('loading'));
      // Loading should not cause expanded message
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      assert.equal(window.getComputedStyle(messageBar).pointerEvents, 'none');

      connectionStateStore.state = ConnectionState.CONNECTED;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
      // Message did change from before loading, should cause expanded
      assert.isTrue(connectionIndicator.hasAttribute('expanded'));
      assert.isTrue(isPopoverOpen());
      await sleep(20);
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.isFalse(isPopoverOpen());
      assert.equal(window.getComputedStyle(messageBar).pointerEvents, 'none');
    });

    it('should not react on store state change after removed from DOM', async () => {
      await setupIndicator();
      connectionIndicator.remove();

      connectionStateStore.state = ConnectionState.LOADING;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);

      connectionStateStore.state = ConnectionState.RECONNECTING;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);

      connectionStateStore.state = ConnectionState.CONNECTION_LOST;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);

      connectionStateStore.state = ConnectionState.CONNECTED;
      await connectionIndicator.updateComplete;
      assert.isFalse(connectionIndicator.hasAttribute('offline'));
      assert.isFalse(connectionIndicator.hasAttribute('reconnecting'));
      assert.isFalse(connectionIndicator.hasAttribute('loading'));
      assert.isFalse(connectionIndicator.hasAttribute('expanded'));
      assert.equal(String(message.textContent).trim(), connectionIndicator.onlineText);
    });

    it('should show loading using delays', async () => {
      await setupIndicator();
      connectionIndicator.firstDelay = 100;
      connectionIndicator.secondDelay = 200;
      connectionIndicator.thirdDelay = 400;

      const loadingBar = connectionIndicator.querySelector('.v-loading-indicator')!;
      assert.equal(loadingBar.getAttribute('style'), 'display: none');

      connectionStateStore.state = ConnectionState.LOADING;
      await connectionIndicator.updateComplete;
      assert.isFalse(loadingBar.classList.contains('first'));
      assert.isFalse(loadingBar.classList.contains('second'));
      assert.isFalse(loadingBar.classList.contains('third'));
      assert.isTrue(isPopoverOpen());

      await sleep(150);
      assert.isTrue(loadingBar.classList.contains('first'));
      assert.isFalse(loadingBar.classList.contains('second'));
      assert.isFalse(loadingBar.classList.contains('third'));
      assert.equal(loadingBar.getAttribute('style'), 'display: block');
      assert.isTrue(isPopoverOpen());

      await sleep(150);
      assert.isFalse(loadingBar.classList.contains('first'));
      assert.isTrue(loadingBar.classList.contains('second'));
      assert.isFalse(loadingBar.classList.contains('third'));
      assert.equal(loadingBar.getAttribute('style'), 'display: block');
      assert.isTrue(isPopoverOpen());

      await sleep(150);
      assert.isFalse(loadingBar.classList.contains('first'));
      assert.isFalse(loadingBar.classList.contains('second'));
      assert.isTrue(loadingBar.classList.contains('third'));
      assert.equal(loadingBar.getAttribute('style'), 'display: block');
      assert.isTrue(isPopoverOpen());

      connectionStateStore.state = ConnectionState.CONNECTED;
      await connectionIndicator.updateComplete;
      assert.isFalse(loadingBar.classList.contains('first'));
      assert.isFalse(loadingBar.classList.contains('second'));
      assert.isFalse(loadingBar.classList.contains('third'));
      assert.equal(loadingBar.getAttribute('style'), 'display: none');
      assert.isFalse(isPopoverOpen());
    });
  });
});
