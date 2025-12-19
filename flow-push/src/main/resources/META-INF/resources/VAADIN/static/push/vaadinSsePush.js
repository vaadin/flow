/**
 * Vaadin SSE Push - Server-Sent Events based push without Atmosphere
 *
 * This module provides an Atmosphere-compatible API using native EventSource
 * for server-to-client push communication. Client-to-server messages use XHR.
 */
window.vaadinSsePush = window.vaadinSsePush || {};

(function() {
    'use strict';

    if (window.console) {
        window.console.debug("Vaadin SSE push loaded");
    }

    /**
     * SSE-based Atmosphere compatibility layer
     */
    var atmosphere = {
        version: "sse-1.0",

        /**
         * Subscribe to SSE push channel
         * @param {Object} options - Configuration options (Atmosphere-compatible)
         * @returns {Object} - Connection object with push() and close() methods
         */
        subscribe: function(options) {
            return new SseConnection(options);
        },

        /**
         * Unsubscribe from URL (no-op for SSE, kept for compatibility)
         */
        unsubscribeUrl: function(url) {
            // SSE connections are closed via the connection object
        }
    };

    /**
     * SSE Connection class - implements Atmosphere-compatible interface
     */
    function SseConnection(options) {
        this.options = options || {};
        this.eventSource = null;
        this.state = 'closed';
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = options.maxReconnectOnClose || 5;
        this.reconnectInterval = options.reconnectInterval || 0;

        // Build SSE URL
        this.url = this.buildSseUrl(options.url);

        // Start connection
        this.connect();
    }

    SseConnection.prototype.buildSseUrl = function(baseUrl) {
        // Convert push URL to SSE URL by changing request type from 'push' to 'sse'
        var url = baseUrl;
        if (url.indexOf('v-r=push') !== -1) {
            url = url.replace('v-r=push', 'v-r=sse');
        } else if (url.indexOf('?') !== -1) {
            url += '&v-r=sse';
        } else {
            url += '?v-r=sse';
        }
        return url;
    };

    SseConnection.prototype.connect = function() {
        var self = this;

        if (this.eventSource) {
            this.eventSource.close();
        }

        try {
            this.eventSource = new EventSource(this.url, {
                withCredentials: true
            });

            this.eventSource.onopen = function(event) {
                self.state = 'connected';
                self.reconnectAttempts = 0;

                var response = self.createResponse('open');
                response.transport = 'sse';
                response.state = 'opening';

                if (typeof self.options.onOpen === 'function') {
                    self.options.onOpen(response);
                }
            };

            this.eventSource.onmessage = function(event) {
                self.handleMessage(event.data);
            };

            // Handle custom 'uidl' events from server
            this.eventSource.addEventListener('uidl', function(event) {
                self.handleMessage(event.data);
            });

            // Handle 'connected' event from server
            this.eventSource.addEventListener('connected', function(event) {
                if (window.console) {
                    window.console.debug('SSE connected event:', event.data);
                }
            });

            // Handle 'sessionExpired' event from server
            this.eventSource.addEventListener('sessionExpired', function(event) {
                if (window.console) {
                    window.console.debug('SSE session expired');
                }
                var response = self.createResponse('error');
                response.status = 401;
                response.error = 'Session expired';
                if (typeof self.options.onError === 'function') {
                    self.options.onError(response);
                }
                self.close();
            });

            this.eventSource.onerror = function(event) {
                if (self.state === 'closed') {
                    return; // Intentionally closed, don't trigger error
                }

                var response = self.createResponse('error');

                // EventSource automatically reconnects, but we track attempts
                if (self.eventSource.readyState === EventSource.CLOSED) {
                    self.state = 'closed';

                    if (self.reconnectAttempts < self.maxReconnectAttempts) {
                        self.reconnectAttempts++;
                        response.state = 'error';

                        if (typeof self.options.onReconnect === 'function') {
                            self.options.onReconnect(response, {
                                timeout: self.reconnectInterval,
                                attempts: self.reconnectAttempts
                            });
                        }
                    } else {
                        response.state = 'error';
                        if (typeof self.options.onError === 'function') {
                            self.options.onError(response);
                        }
                        if (typeof self.options.onClose === 'function') {
                            self.options.onClose(response);
                        }
                    }
                } else if (self.eventSource.readyState === EventSource.CONNECTING) {
                    // Browser is reconnecting
                    response.state = 'error';
                    if (typeof self.options.onError === 'function') {
                        self.options.onError(response);
                    }
                }
            };

        } catch (e) {
            if (window.console) {
                window.console.error('Failed to create EventSource:', e);
            }
            var response = this.createResponse('error');
            response.error = e.message;
            if (typeof this.options.onTransportFailure === 'function') {
                this.options.onTransportFailure(e.message, response);
            }
        }
    };

    SseConnection.prototype.handleMessage = function(data) {
        var response = this.createResponse('message');
        response.responseBody = data;
        response.state = 'messageReceived';
        response.status = 200;

        if (typeof this.options.onMessage === 'function') {
            this.options.onMessage(response);
        }
    };

    SseConnection.prototype.createResponse = function(type) {
        return {
            request: this.options,
            transport: 'sse',
            state: type,
            status: 200,
            responseBody: '',
            headers: function(name) { return null; },
            closedByClientTimeout: false
        };
    };

    /**
     * Push a message to the server via XHR (SSE is server-to-client only)
     * @param {string} message - Message to send
     */
    SseConnection.prototype.push = function(message) {
        // SSE is unidirectional (server-to-client), so we use XHR for client-to-server
        // The message is sent as a regular UIDL request
        var xhr = new XMLHttpRequest();
        var url = this.options.url;

        // Make sure we use the UIDL endpoint, not SSE
        if (url.indexOf('v-r=sse') !== -1) {
            url = url.replace('v-r=sse', 'v-r=uidl');
        } else if (url.indexOf('v-r=push') !== -1) {
            url = url.replace('v-r=push', 'v-r=uidl');
        }

        xhr.open('POST', url, true);
        xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');

        // Add custom headers if defined
        if (this.options.headers) {
            for (var header in this.options.headers) {
                if (this.options.headers.hasOwnProperty(header)) {
                    var value = this.options.headers[header];
                    if (typeof value === 'function') {
                        value = value();
                    }
                    xhr.setRequestHeader(header, value);
                }
            }
        }

        var self = this;
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                if (xhr.status >= 200 && xhr.status < 300) {
                    // Success - any response is handled via SSE
                } else {
                    if (window.console) {
                        window.console.warn('SSE XHR push failed:', xhr.status);
                    }
                    var response = self.createResponse('error');
                    response.status = xhr.status;
                    if (typeof self.options.onError === 'function') {
                        self.options.onError(response);
                    }
                }
            }
        };

        xhr.send(message);
    };

    /**
     * Close the SSE connection
     */
    SseConnection.prototype.close = function() {
        this.state = 'closed';
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        var response = this.createResponse('closed');
        response.state = 'closed';
        if (typeof this.options.onClose === 'function') {
            this.options.onClose(response);
        }
    };

    // Expose atmosphere-compatible API
    window.vaadinSsePush.atmosphere = atmosphere;

    // Also expose as vaadinPush for compatibility with FlowClient.js
    // which expects window.vaadinPush.atmosphere
    window.vaadinPush = window.vaadinPush || {};
    window.vaadinPush.atmosphere = atmosphere;

})();
