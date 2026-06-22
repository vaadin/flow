import { expect } from '@open-wc/testing';
import { doDisconnect, doPush, isAtmosphereLoaded } from '../../main/frontend/internal/AtmospherePushConnection';

describe('AtmospherePushConnection', () => {
  const win = window as unknown as { vaadinPush?: unknown };
  let saved: unknown;
  beforeEach(() => {
    saved = win.vaadinPush;
  });
  afterEach(() => {
    win.vaadinPush = saved;
  });

  it('isAtmosphereLoaded reflects window.vaadinPush.atmosphere', () => {
    win.vaadinPush = undefined;
    expect(isAtmosphereLoaded()).to.be.false;
    win.vaadinPush = { atmosphere: { subscribe: () => ({}), unsubscribeUrl: () => {} } };
    expect(isAtmosphereLoaded()).to.be.true;
  });

  it('doPush pushes the message over the socket', () => {
    let pushed: string | undefined;
    const socket = {
      push: (message: string) => {
        pushed = message;
      }
    };
    doPush(socket, 'hello');
    expect(pushed).to.equal('hello');
  });

  it('doDisconnect unsubscribes the url via atmosphere', () => {
    let unsubscribed: string | undefined;
    win.vaadinPush = {
      atmosphere: {
        subscribe: () => ({}),
        unsubscribeUrl: (url: string) => {
          unsubscribed = url;
        }
      }
    };
    doDisconnect('http://localhost/push');
    expect(unsubscribed).to.equal('http://localhost/push');
  });
});
