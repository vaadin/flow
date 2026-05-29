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
package com.vaadin.flow.micrometer.client;

/**
 * Trivial token bucket: refills {@code ratePerWindow} tokens every
 * {@code windowMs} milliseconds. Not thread safe in the strict sense, but
 * synchronized on the instance, which is sufficient because there is one
 * limiter per UI.
 */
final class ClientRateLimiter {

    private static final long WINDOW_MS = 10_000L;

    private final int ratePerWindow;
    private long windowStart;
    private int consumed;

    ClientRateLimiter(int ratePerWindow) {
        this.ratePerWindow = ratePerWindow;
        this.windowStart = System.currentTimeMillis();
    }

    /**
     * Attempts to consume {@code n} tokens. Returns the number actually granted
     * (between 0 and {@code n}); the caller must drop any samples beyond that.
     */
    synchronized int tryAcquire(int n) {
        if (ratePerWindow <= 0) {
            return n;
        }
        long now = System.currentTimeMillis();
        if (now - windowStart >= WINDOW_MS) {
            windowStart = now;
            consumed = 0;
        }
        int remaining = Math.max(0, ratePerWindow - consumed);
        int granted = Math.min(n, remaining);
        consumed += granted;
        return granted;
    }
}
