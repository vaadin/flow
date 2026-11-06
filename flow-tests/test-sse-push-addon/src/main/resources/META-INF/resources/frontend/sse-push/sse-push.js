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

/*
 * Client side of the SSE push transport add-on. Messages from the server are
 * streamed over EventSource, messages to the server are sent by the client
 * engine as regular requests.
 *
 * The frontend bundle includes this module, and the client engine uses the
 * transport it registers as window.Vaadin.Flow.pushTransport. The transport
 * implements the subset of the Atmosphere API that the push connection of the client engine
 * uses: subscribe(request) and unsubscribeUrl(url).
 */
(function () {
  const NAME = 'sse-addon';

  // How long a stream must stay open before its reconnect attempts start
  // over, so that a stream that ends right after opening, e.g. closed by a
  // proxy, does not reconnect forever
  const STABLE_CONNECTION_MS = 10000;

  function clearTimer(timer) {
    if (timer) {
      clearTimeout(timer);
    }
    return null;
  }

  class SseConnection {
    constructor(options) {
      this.options = options;
      this.eventSource = null;
      this.state = 'closed';
      this.reconnectAttempts = 0;
      this.maxReconnectAttempts = options.maxReconnectOnClose ?? 25;
      this.reconnectInterval = options.reconnectInterval ?? 5000;
      this.reconnectTimeoutId = null;
      this.stableTimeoutId = null;
      this.watchdogTimeoutId = null;
      // Seconds between keep-alive events of the server, 0 if not sent
      this.keepAliveInterval = 0;
      // The push connection builds the url of an Atmosphere push request
      this.url = options.url.replace('v-r=push', 'v-r=sse');
      this.connect();
    }

    connect() {
      this.stopStream();
      try {
        this.state = 'connecting';
        this.eventSource = new EventSource(this.url, { withCredentials: true });

        this.eventSource.onopen = () => {
          this.state = 'connected';
          this.stableTimeoutId = setTimeout(() => {
            this.stableTimeoutId = null;
            this.reconnectAttempts = 0;
          }, STABLE_CONNECTION_MS);
          this.call('onOpen', this.createResponse('opening'));
        };

        this.eventSource.onmessage = (event) => {
          this.watchStream();
          const response = this.createResponse('messageReceived');
          response.responseBody = event.data;
          this.call('onMessage', response);
        };

        // EventSource does not pass comments on, so the server keeps the
        // stream alive with events that carry the interval in seconds
        this.eventSource.addEventListener('keep-alive', (event) => {
          this.keepAliveInterval = Number(event.data) || 0;
          this.watchStream();
        });

        this.eventSource.onerror = () => this.streamFailed();
      } catch (e) {
        const response = this.createResponse('error');
        response.error = e instanceof Error ? e.message : String(e);
        if (this.options.onTransportFailure) {
          this.options.onTransportFailure(response.error, response);
        }
      }
    }

    streamFailed() {
      if (this.state === 'closed') {
        return;
      }
      // The reconnecting of EventSource differs between browsers, so the
      // stream is reopened here instead
      this.stopStream();
      this.state = 'closed';
      this.scheduleReconnect();
    }

    // Reconnects if neither an event nor a keep-alive arrives for two
    // keep-alive intervals. A stream can die without an error, e.g. when the
    // network changes or a proxy drops the connection silently.
    watchStream() {
      this.watchdogTimeoutId = clearTimer(this.watchdogTimeoutId);
      if (this.keepAliveInterval > 0) {
        this.watchdogTimeoutId = setTimeout(() => {
          this.watchdogTimeoutId = null;
          this.streamFailed();
        }, this.keepAliveInterval * 2000);
      }
    }

    stopStream() {
      this.stableTimeoutId = clearTimer(this.stableTimeoutId);
      this.watchdogTimeoutId = clearTimer(this.watchdogTimeoutId);
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
    }

    scheduleReconnect() {
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        const response = this.createResponse('error');
        response.error = 'Max reconnection attempts exceeded';
        this.call('onError', response);
        this.call('onClose', response);
        return;
      }

      this.reconnectAttempts += 1;
      // Tells the client engine that the connection is being re-established
      if (this.options.onReconnect) {
        this.options.onReconnect(this.options, this.createResponse('reconnect'));
      }

      // The first attempts come quickly, the later ones less often
      const interval = this.reconnectAttempts <= 3 ? 1000 : this.reconnectInterval;
      this.reconnectTimeoutId = setTimeout(() => {
        this.reconnectTimeoutId = null;
        if (this.state === 'closed') {
          this.connect();
        }
      }, interval);
    }

    call(callback, response) {
      if (this.options[callback]) {
        this.options[callback](response);
      }
    }

    createResponse(state) {
      return {
        request: this.options,
        transport: NAME,
        state,
        status: 200,
        responseBody: '',
        headers: () => null,
        closedByClientTimeout: false
      };
    }

    close() {
      this.state = 'closed';
      this.reconnectTimeoutId = clearTimer(this.reconnectTimeoutId);
      this.stopStream();
      this.call('onClose', this.createResponse('closed'));
    }
  }

  // The push connection disconnects by url, like with Atmosphere
  const connections = new Map();

  window.Vaadin = window.Vaadin || {};
  window.Vaadin.Flow = window.Vaadin.Flow || {};
  window.Vaadin.Flow.pushTransport = {
    subscribe(request) {
      const connection = new SseConnection(request);
      connections.set(request.url, connection);
      return connection;
    },
    unsubscribeUrl(url) {
      const connection = connections.get(url);
      connections.delete(url);
      if (connection) {
        connection.close();
      }
    }
  };
})();
