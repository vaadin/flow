import { expect } from '@open-wc/testing';
import sinon from 'sinon';

// API under test
import {
  createSsePushAtmosphere,
  SSE_PURE_TRANSPORT,
  SseConnection,
  type SseAtmosphereRequest,
  type SseAtmosphereResponse
} from '../../../../../main/frontend/internal/client/communication/SsePush';

import { FakeEventSource } from './fakeEventSource';

const $wnd = window as any;

function recordingRequest(url = 'VAADIN/push?v-r=push&v-uiId=0'): {
  request: SseAtmosphereRequest;
  calls: Array<{ callback: string; response: SseAtmosphereResponse }>;
} {
  const calls: Array<{ callback: string; response: SseAtmosphereResponse }> = [];
  const record = (callback: string) => (response: SseAtmosphereResponse) => calls.push({ callback, response });
  const request: SseAtmosphereRequest = {
    url,
    onOpen: record('onOpen'),
    onMessage: record('onMessage'),
    onError: record('onError'),
    onClose: record('onClose'),
    onReconnect: (_request, response) => calls.push({ callback: 'onReconnect', response }),
    onTransportFailure: (_message, response) => calls.push({ callback: 'onTransportFailure', response })
  };
  return { request, calls };
}

function callbacks(calls: Array<{ callback: string }>): string[] {
  return calls.map((call) => call.callback);
}

describe('SsePush', () => {
  let originalEventSource: typeof EventSource;
  let originalVaadinPush: unknown;
  let clock: sinon.SinonFakeTimers;

  beforeEach(() => {
    originalEventSource = window.EventSource;
    originalVaadinPush = $wnd.vaadinPush;
    $wnd.EventSource = FakeEventSource;
    FakeEventSource.instances = [];
    clock = sinon.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
  });

  afterEach(() => {
    clock.restore();
    $wnd.EventSource = originalEventSource;
    $wnd.vaadinPush = originalVaadinPush;
  });

  describe('SseConnection', () => {
    it('opens an event stream with the sse request type and credentials', () => {
      new SseConnection(recordingRequest('VAADIN/push?v-r=push&v-uiId=0&v-pushId=abc').request);

      expect(FakeEventSource.instances).to.have.length(1);
      expect(FakeEventSource.latest().url).to.equal('VAADIN/push?v-r=sse&v-uiId=0&v-pushId=abc');
      expect(FakeEventSource.latest().withCredentials).to.be.true;
    });

    it('adds the sse request type to a url without one', () => {
      new SseConnection(recordingRequest('VAADIN/push').request);
      expect(FakeEventSource.latest().url).to.equal('VAADIN/push?v-r=sse');

      new SseConnection(recordingRequest('VAADIN/push?v-uiId=0').request);
      expect(FakeEventSource.latest().url).to.equal('VAADIN/push?v-uiId=0&v-r=sse');
    });

    it('reports the open stream with the sse-pure transport', () => {
      const { request, calls } = recordingRequest();
      new SseConnection(request);

      FakeEventSource.latest().open();

      expect(callbacks(calls)).to.deep.equal(['onOpen']);
      expect(calls[0].response.transport).to.equal(SSE_PURE_TRANSPORT);
      expect(calls[0].response.request).to.equal(request);
    });

    it('passes uidl events to the message callback', () => {
      const { request, calls } = recordingRequest();
      new SseConnection(request);
      FakeEventSource.latest().open();

      FakeEventSource.latest().emit('uidl', '{"syncId":1}');

      expect(callbacks(calls)).to.deep.equal(['onOpen', 'onMessage']);
      expect(calls[1].response.responseBody).to.equal('{"syncId":1}');
      expect(calls[1].response.status).to.equal(200);
      expect(calls[1].response.transport).to.equal(SSE_PURE_TRANSPORT);
    });

    it('passes unnamed events to the message callback', () => {
      const { request, calls } = recordingRequest();
      new SseConnection(request);

      FakeEventSource.latest().emit('message', '{"syncId":2}');

      expect(callbacks(calls)).to.deep.equal(['onMessage']);
      expect(calls[0].response.responseBody).to.equal('{"syncId":2}');
    });

    it('ignores the connected event', () => {
      const { request, calls } = recordingRequest();
      new SseConnection(request);

      FakeEventSource.latest().emit('connected', '{"uiId":0}');

      expect(calls).to.be.empty;
    });

    it('reconnects with a new event stream when the stream fails', () => {
      const { request, calls } = recordingRequest();
      new SseConnection(request);
      const first = FakeEventSource.latest();
      first.open();

      first.fail();

      // The browser's own reconnect is not used, as it differs between
      // browsers
      expect(first.closed).to.be.true;
      expect(callbacks(calls)).to.deep.equal(['onOpen', 'onReconnect']);
      expect(FakeEventSource.instances).to.have.length(1);

      clock.tick(1000);

      expect(FakeEventSource.instances).to.have.length(2);
      expect(FakeEventSource.latest().url).to.equal(first.url);

      FakeEventSource.latest().open();
      expect(callbacks(calls)).to.deep.equal(['onOpen', 'onReconnect', 'onOpen']);
    });

    it('waits the reconnect interval after the first quick attempts', () => {
      const { request } = recordingRequest();
      request.reconnectInterval = 5000;
      new SseConnection(request);

      for (let attempt = 1; attempt <= 3; attempt++) {
        FakeEventSource.latest().fail();
        clock.tick(1000);
        expect(FakeEventSource.instances).to.have.length(attempt + 1);
      }

      FakeEventSource.latest().fail();
      clock.tick(4999);
      expect(FakeEventSource.instances).to.have.length(4);
      clock.tick(1);
      expect(FakeEventSource.instances).to.have.length(5);
    });

    it('resets the reconnect attempts when the stream opens again', () => {
      const { request, calls } = recordingRequest();
      request.maxReconnectOnClose = 2;
      new SseConnection(request);

      for (let i = 0; i < 5; i++) {
        FakeEventSource.latest().fail();
        clock.tick(1000);
        FakeEventSource.latest().open();
      }

      expect(callbacks(calls)).not.to.include('onError');
      expect(FakeEventSource.instances).to.have.length(6);
    });

    it('gives up after the maximum number of reconnect attempts', () => {
      const { request, calls } = recordingRequest();
      request.maxReconnectOnClose = 2;
      new SseConnection(request);

      FakeEventSource.latest().fail();
      clock.tick(1000);
      FakeEventSource.latest().fail();
      clock.tick(1000);
      FakeEventSource.latest().fail();
      clock.tick(10000);

      expect(FakeEventSource.instances).to.have.length(3);
      expect(callbacks(calls)).to.deep.equal(['onReconnect', 'onReconnect', 'onError', 'onClose']);
    });

    it('closes the event stream and reports the close', () => {
      const { request, calls } = recordingRequest();
      const connection = new SseConnection(request);
      FakeEventSource.latest().open();

      connection.close();

      expect(FakeEventSource.latest().closed).to.be.true;
      expect(callbacks(calls)).to.deep.equal(['onOpen', 'onClose']);
    });

    it('does not reconnect after being closed', () => {
      const { request, calls } = recordingRequest();
      const connection = new SseConnection(request);
      FakeEventSource.latest().fail();

      connection.close();
      clock.tick(10000);

      expect(FakeEventSource.instances).to.have.length(1);
      expect(callbacks(calls)).to.deep.equal(['onReconnect', 'onClose']);
    });

    it('ignores errors of a closed stream', () => {
      const { request, calls } = recordingRequest();
      const connection = new SseConnection(request);
      const stream = FakeEventSource.latest();
      connection.close();

      stream.fail();
      clock.tick(10000);

      expect(FakeEventSource.instances).to.have.length(1);
      expect(callbacks(calls)).to.deep.equal(['onClose']);
    });

    it('reports a transport failure when the event stream cannot be created', () => {
      $wnd.EventSource = function () {
        throw new Error('not supported');
      };
      const { request, calls } = recordingRequest();

      new SseConnection(request);

      expect(callbacks(calls)).to.deep.equal(['onTransportFailure']);
      expect(calls[0].response.error).to.equal('not supported');
    });
  });

  describe('createSsePushAtmosphere', () => {
    it('does not install itself as the global Atmosphere', () => {
      $wnd.vaadinPush = undefined;

      createSsePushAtmosphere();

      expect($wnd.vaadinPush).to.be.undefined;
    });

    it('subscribes by opening an SSE connection', () => {
      const atmosphere = createSsePushAtmosphere();
      const { request } = recordingRequest();

      const connection = atmosphere.subscribe(request);

      expect(connection).to.be.instanceOf(SseConnection);
      expect(FakeEventSource.instances).to.have.length(1);
    });

    it('closes the connection subscribed with the url on unsubscribe', () => {
      const atmosphere = createSsePushAtmosphere();
      const { request, calls } = recordingRequest();
      atmosphere.subscribe(request);
      FakeEventSource.latest().open();

      atmosphere.unsubscribeUrl(request.url);

      expect(FakeEventSource.latest().closed).to.be.true;
      expect(callbacks(calls)).to.deep.equal(['onOpen', 'onClose']);
    });

    it('ignores unsubscribe of an unknown url', () => {
      const atmosphere = createSsePushAtmosphere();
      const { request, calls } = recordingRequest();
      atmosphere.subscribe(request);

      atmosphere.unsubscribeUrl('VAADIN/push?v-r=push&v-uiId=1');

      expect(FakeEventSource.latest().closed).to.be.false;
      expect(calls).to.be.empty;
    });
  });
});
