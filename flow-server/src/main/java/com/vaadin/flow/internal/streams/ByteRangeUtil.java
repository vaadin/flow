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
package com.vaadin.flow.internal.streams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.vaadin.flow.external.jetty.http.ByteRange;
import com.vaadin.flow.server.VaadinRequest;

/**
 * Helpers for answering HTTP range requests ({@code Range: bytes=...}). The
 * header itself is parsed by {@link ByteRange}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ByteRangeUtil {

    private ByteRangeUtil() {
        // Static utils only
    }

    /**
     * Checks whether the response to a request should be a range response.
     * <p>
     * Content of unknown length is always sent whole. So is content requested
     * with an {@code If-Range} condition, since no validator is sent that the
     * condition could match, and content requested in a unit other than bytes.
     *
     * @param request
     *            the request
     * @param contentLength
     *            the length of the whole content, or {@code -1} if unknown
     * @return {@code true} if the {@code Range} header of the request should be
     *         answered, {@code false} to send the whole content
     */
    public static boolean isRangeRequest(VaadinRequest request,
            long contentLength) {
        String range = request.getHeader("Range");
        return contentLength >= 0 && request.getHeader("If-Range") == null
                && range != null && range.trim().startsWith("bytes=");
    }

    /**
     * Wraps a stream so that it ends after the given number of bytes. Closing
     * the returned stream closes the wrapped one.
     *
     * @param inputStream
     *            the stream to read from
     * @param length
     *            the maximum number of bytes to read
     * @return a stream that reads at most {@code length} bytes
     */
    public static InputStream limit(InputStream inputStream, long length) {
        return new LimitedInputStream(inputStream, length);
    }

    private static class LimitedInputStream extends FilterInputStream {
        private long remaining;

        private LimitedInputStream(InputStream in, long length) {
            super(in);
            remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = super.read();
            if (read >= 0) {
                remaining--;
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            if (remaining <= 0) {
                return -1;
            }
            int read = super.read(b, off, (int) Math.min(len, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
