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
  private reconnectTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private url: string;

  constructor(options: SseAtmosphereRequest) {
    this.options = options;
    this.maxReconnectAttempts = options.maxReconnectOnClose ?? 25;
    this.reconnectInterval = options.reconnectInterval ?? 5000;
    this.url = this.buildSseUrl(options.url);
    this.connect();
  }

  private buildSseUrl(baseUrl: string): string {
    let url = baseUrl;
    if (url.includes('v-r=push')) {
      url = url.replace('v-r=push', 'v-r=sse');
    } else if (url.includes('?')) {
      url += '&v-r=sse';
    } else {
      url += '?v-r=sse';
    }
    return url;
  }

  private connect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    try {
      this.state = 'connecting';
      this.eventSource = new EventSource(this.url, { withCredentials: true });

      this.eventSource.onopen = () => {
        this.state = 'connected';
        this.reconnectAttempts = 0;
        const response = this.createResponse('open');
        response.transport = SSE_PURE_TRANSPORT;
        response.state = 'opening';
        this.options.onOpen?.(response);
      };

      this.eventSource.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.eventSource.addEventListener('uidl', (event) => {
        this.handleMessage((event as MessageEvent).data);
      });

      this.eventSource.onerror = () => {
        if (this.state === 'closed') return;

        // Don't rely on browser's auto-reconnection - it's inconsistent across
        // browsers (Safari, Chrome, Firefox behave differently). Take full
        // control of reconnection by closing the EventSource and managing
        // reconnection ourselves.
        if (this.eventSource) {
          this.eventSource.close();
          this.eventSource = null;
        }
        this.state = 'closed';
        this.scheduleReconnect();
      };
    } catch (e) {
      const response = this.createResponse('error');
      response.error = e instanceof Error ? e.message : String(e);
      this.options.onTransportFailure?.(response.error, response);
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

  private handleMessage(data: string): void {
    const response = this.createResponse('message');
    response.responseBody = data;
    response.state = 'messageReceived';
    response.status = 200;
    this.options.onMessage?.(response);
  }

  private createResponse(type: string): SseAtmosphereResponse {
    return {
      request: this.options,
      transport: SSE_PURE_TRANSPORT,
      state: type,
      status: 200,
      responseBody: '',
      headers: () => null,
      closedByClientTimeout: false
    };
  }

  close(): void {
    this.state = 'closed';
    if (this.reconnectTimeoutId) {
      clearTimeout(this.reconnectTimeoutId);
      this.reconnectTimeoutId = null;
    }
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    const response = this.createResponse('closed');
    response.state = 'closed';
    this.options.onClose?.(response);
  }
}

/**
 * The SSE_PURE transport behind the Atmosphere API used by the push
 * connection.
 */
export interface SsePushAtmosphere {
  subscribe(options: SseAtmosphereRequest): SseConnection;
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
    subscribe(options: SseAtmosphereRequest): SseConnection {
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
