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
 * Client side of the Atmosphere-free SSE_PURE push transport. Messages from the
 * server are streamed over EventSource, messages to the server are sent as
 * regular UIDL requests, as the connection is not bidirectional.
 *
 * The transport implements the subset of the Atmosphere API that
 * AtmospherePushConnection uses, so the regular push connection of the client
 * engine works on top of it without loading Atmosphere.
 */

/** The transport name the connection reports to the client engine. */
export const SSE_PURE_TRANSPORT = 'sse-pure';

/**
 * How long a stream must stay open before its reconnect attempts start over,
 * so that a stream that ends right after opening, e.g. closed by a proxy, does
 * not reconnect forever.
 */
const STABLE_CONNECTION_MS = 10000;

type Timer = ReturnType<typeof setTimeout> | null;

function clearTimer(timer: Timer): null {
  if (timer) {
    clearTimeout(timer);
  }
  return null;
}

export interface SseAtmosphereResponse {
  request: SseAtmosphereRequest;
  transport: string;
  state: string;
  status: number;
  responseBody: string;
  headers: (name: string) => string | null;
  closedByClientTimeout: boolean;
  error?: string;
}

export interface SseAtmosphereRequest {
  url: string;
  maxReconnectOnClose?: number;
  reconnectInterval?: number;
  headers?: Record<string, string | (() => string)>;
  onOpen?: (response: SseAtmosphereResponse) => void;
  onMessage?: (response: SseAtmosphereResponse) => void;
  onError?: (response: SseAtmosphereResponse) => void;
  onClose?: (response: SseAtmosphereResponse) => void;
  onReconnect?: (request: SseAtmosphereRequest, response: SseAtmosphereResponse) => void;
  onTransportFailure?: (errorMsg: string, response: SseAtmosphereResponse) => void;
}

export class SseConnection {
  private options: SseAtmosphereRequest;
  private eventSource: EventSource | null = null;
  private state: 'closed' | 'connecting' | 'connected' = 'closed';
  private reconnectAttempts = 0;
  private maxReconnectAttempts: number;
  private reconnectInterval: number;
  private reconnectTimeoutId: Timer = null;
  private stableTimeoutId: Timer = null;
  private watchdogTimeoutId: Timer = null;
  // Seconds between keep-alive events of the server, 0 if not sent
  private keepAliveInterval = 0;
  private url: string;

  constructor(options: SseAtmosphereRequest) {
    this.options = options;
    this.maxReconnectAttempts = options.maxReconnectOnClose ?? 25;
    this.reconnectInterval = options.reconnectInterval ?? 5000;
    // The push connection builds the url of an Atmosphere push request
    this.url = options.url.replace('v-r=push', 'v-r=sse');
    this.connect();
  }

  private connect(): void {
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
        this.options.onOpen?.(this.createResponse('opening'));
      };

      // The server ends the stream at the heartbeat interval, so that a new
      // request keeps the session and the UI alive
      this.eventSource.addEventListener('reopen', () => this.connect());

      this.eventSource.onmessage = (event) => {
        this.watchStream();
        const response = this.createResponse('messageReceived');
        response.responseBody = event.data;
        this.options.onMessage?.(response);
      };

      // EventSource does not pass comments on, so the server keeps the
      // stream alive with events that carry the interval in seconds
      this.eventSource.addEventListener('keep-alive', (event) => {
        this.keepAliveInterval = Number((event as MessageEvent).data) || 0;
        this.watchStream();
      });

      this.eventSource.onerror = () => this.streamFailed();
    } catch (e) {
      const response = this.createResponse('error');
      response.error = e instanceof Error ? e.message : String(e);
      this.options.onTransportFailure?.(response.error, response);
    }
  }

  private streamFailed(): void {
    if (this.state === 'closed') return;

    // Don't rely on browser's auto-reconnection - it's inconsistent across
    // browsers (Safari, Chrome, Firefox behave differently). Take full
    // control of reconnection by closing the EventSource and managing
    // reconnection ourselves.
    this.stopStream();
    this.state = 'closed';
    this.scheduleReconnect();
  }

  /**
   * Reconnects if neither an event nor a keep-alive arrives for two keep-alive
   * intervals. A stream can die without an error, e.g. when the network
   * changes or a proxy drops the connection silently.
   */
  private watchStream(): void {
    this.watchdogTimeoutId = clearTimer(this.watchdogTimeoutId);
    if (this.keepAliveInterval > 0) {
      this.watchdogTimeoutId = setTimeout(() => {
        this.watchdogTimeoutId = null;
        this.streamFailed();
      }, this.keepAliveInterval * 2000);
    }
  }

  private stopStream(): void {
    this.stableTimeoutId = clearTimer(this.stableTimeoutId);
    this.watchdogTimeoutId = clearTimer(this.watchdogTimeoutId);
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      // Max attempts exceeded - report error
      const response = this.createResponse('error');
      response.error = 'Max reconnection attempts exceeded';
      this.options.onError?.(response);
      this.options.onClose?.(response);
      return;
    }

    this.reconnectAttempts += 1;
    const response = this.createResponse('reconnect');

    // Notify about reconnection attempt (this integrates with Vaadin's connection state)
    this.options.onReconnect?.(this.options, response);

    // Schedule actual reconnection - use shorter interval for first few attempts
    const interval = this.reconnectAttempts <= 3 ? 1000 : this.reconnectInterval;
    this.reconnectTimeoutId = setTimeout(() => {
      this.reconnectTimeoutId = null;
      if (this.state !== 'closed') return; // Already reconnected or closed by user
      this.connect();
    }, interval);
  }

  private createResponse(state: string): SseAtmosphereResponse {
    return {
      request: this.options,
      transport: SSE_PURE_TRANSPORT,
      state,
      status: 200,
      responseBody: '',
      headers: () => null,
      closedByClientTimeout: false
    };
  }

  close(): void {
    this.state = 'closed';
    this.reconnectTimeoutId = clearTimer(this.reconnectTimeoutId);
    this.stopStream();
    this.options.onClose?.(this.createResponse('closed'));
  }
}

/**
 * The SSE_PURE transport behind the Atmosphere API used by the push
 * connection.
 */
export interface SsePushAtmosphere {
  subscribe(config: unknown): SseConnection;
  unsubscribeUrl(url: string): void;
}

/**
 * Creates the SSE_PURE transport with the Atmosphere API used by the push
 * connection.
 *
 * @returns the transport
 */
export function createSsePushAtmosphere(): SsePushAtmosphere {
  // The push connection disconnects by url, like with Atmosphere
  const connections = new Map<string, SseConnection>();
  return {
    subscribe(config: unknown): SseConnection {
      // The push connection passes the callbacks of the request in the config
      const options = config as SseAtmosphereRequest;
      const connection = new SseConnection(options);
      connections.set(options.url, connection);
      return connection;
    },
    unsubscribeUrl(url: string): void {
      const connection = connections.get(url);
      connections.delete(url);
      connection?.close();
    }
  };
}
