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
package com.vaadin.flow.devloop.daemon;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The daemon's end of the in-app connector's registration connection.
 * <p>
 * One connection carries both directions: the connector opens it and holds it
 * for the app's lifetime, the daemon sends commands down it and reads the
 * reply. That keeps the app-liveness signal and the command channel as one
 * thing, so there is no second socket to get out of step with the first.
 * <p>
 * Commands are serialised because at most one transaction is ever in flight;
 * anything else would make the reply ambiguous.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class Connector {

    private final PrintWriter out;
    private final BlockingQueue<String> replies = new ArrayBlockingQueue<>(1);
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean closed;

    Connector(PrintWriter out) {
        this.out = out;
    }

    /** Called by the reader thread for every line the connector sends. */
    void onLine(String line) {
        // Drop anything nobody is waiting for rather than blocking the reader.
        replies.offer(line);
    }

    void close() {
        closed = true;
        replies.offer("ERR kind=app-gone message=registration-closed");
    }

    boolean isOpen() {
        return !closed;
    }

    /**
     * Sends a command and waits for the single reply line. The timeout is a
     * backstop against a wedged app, not the normal path: a redefine answers in
     * milliseconds, and an app that died closes the connection, which unblocks
     * this immediately.
     */
    Optional<String> command(String request, long timeoutSeconds) {
        if (closed) {
            return Optional.empty();
        }
        lock.lock();
        try {
            replies.clear();
            out.println(request);
            if (out.checkError()) {
                return Optional.empty();
            }
            String reply = replies.poll(timeoutSeconds, TimeUnit.SECONDS);
            return Optional.ofNullable(reply);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    /** Parses the connector's {@code key=value} reply into a map. */
    static Map<String, String> fields(String reply) {
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        String line = reply.trim();

        // "message=" carries free text (the JVM's own rejection reason, which
        // contains spaces), so it is always last and takes the rest of the
        // line.
        int messageAt = line.indexOf("message=");
        if (messageAt >= 0) {
            fields.put("message",
                    line.substring(messageAt + "message=".length()).trim());
            line = line.substring(0, messageAt).trim();
        }

        String[] parts = line.split("\\s+");
        fields.put("status", parts.length > 0 ? parts[0] : "");
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0) {
                fields.put(parts[i].substring(0, eq),
                        parts[i].substring(eq + 1));
            }
        }
        return fields;
    }

    /** Drains lines the connector sends unprompted; keeps the reader simple. */
    static void pump(BufferedReader in, Connector connector,
            Runnable onActivity) throws java.io.IOException {
        String line;
        while ((line = in.readLine()) != null) {
            onActivity.run();
            connector.onLine(line);
        }
    }
}
