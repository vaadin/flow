import { expect } from '@open-wc/testing';
import { sendBeacon } from '../../main/frontend/internal/MessageSender';

describe('MessageSender', () => {
  it('forwards url and payload to navigator.sendBeacon', () => {
    const calls: Array<[string, string]> = [];
    const original = window.navigator.sendBeacon;
    // navigator.sendBeacon is read-only; stub it via defineProperty.
    Object.defineProperty(window.navigator, 'sendBeacon', {
      configurable: true,
      value: (url: string, payload: string) => {
        calls.push([url, payload]);
        return true;
      }
    });
    try {
      sendBeacon('http://localhost/?v-r=unload', '[]');
      expect(calls).to.deep.equal([['http://localhost/?v-r=unload', '[]']]);
    } finally {
      Object.defineProperty(window.navigator, 'sendBeacon', {
        configurable: true,
        value: original
      });
    }
  });
});
