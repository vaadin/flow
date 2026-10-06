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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.flow.server.VaadinRequest;

/**
 * Helpers for answering HTTP range requests ({@code Range: bytes=...}).
 * <p>
 * Only a single byte range is supported. A request for several ranges may be
 * answered with the whole content, which RFC 9110 allows.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ByteRangeUtil {

    // At most 18 digits, so that a value always fits in a long
    private static final Pattern SINGLE_BYTE_RANGE = Pattern
            .compile("bytes=(\\d{0,18})-(\\d{0,18})");

    /**
     * A range of bytes, both ends inclusive.
     *
     * @param start
     *            the index of the first byte
     * @param end
     *            the index of the last byte
     */
    public record ByteRange(long start, long end) {

        /**
         * Gets the number of bytes in the range.
         *
         * @return the number of bytes
         */
        public long length() {
            return end - start + 1;
        }
    }

    private ByteRangeUtil() {
        // Static utils only
    }

    /**
     * Finds the byte range a request asks for.
     * <p>
     * A request with an {@code If-Range} condition gets the whole content,
     * since no validator is sent that the condition could match. Content of
     * unknown length is always sent whole.
     *
     * @param request
     *            the request
     * @param contentLength
     *            the length of the whole content, or {@code -1} if unknown
     * @return the requested range, see {@link #parseRange(String, long)}, or an
     *         empty optional when the whole content should be sent
     */
    public static Optional<ByteRange> parseRange(VaadinRequest request,
            long contentLength) {
        if (contentLength < 0 || request.getHeader("If-Range") != null) {
            return Optional.empty();
        }
        return parseRange(request.getHeader("Range"), contentLength);
    }

    /**
     * Resolves a {@code Range} header against the content length.
     * <p>
     * An open range ({@code bytes=5-}) ends at the last byte, a suffix range
     * ({@code bytes=-5}) covers the last bytes, and an end past the content is
     * cut to the last byte.
     *
     * @param header
     *            the value of the {@code Range} header, or {@code null} if the
     *            request has none
     * @param contentLength
     *            the length of the whole content
     * @return the requested range, which starts at or after
     *         {@code contentLength} when it cannot be satisfied, or an empty
     *         optional when the whole content should be sent: no header,
     *         several ranges, or a malformed range
     */
    public static Optional<ByteRange> parseRange(String header,
            long contentLength) {
        Matcher matcher = header == null ? null
                : SINGLE_BYTE_RANGE.matcher(header.trim());
        if (matcher == null || !matcher.matches()) {
            return Optional.empty();
        }
        String first = matcher.group(1);
        String last = matcher.group(2);
        if (first.isEmpty()) {
            if (last.isEmpty()) {
                return Optional.empty();
            }
            // bytes=-n asks for the last n bytes, none of which exist for n=0
            long suffix = Long.parseLong(last);
            long start = suffix == 0 ? contentLength
                    : Math.max(0, contentLength - suffix);
            return Optional.of(new ByteRange(start, contentLength - 1));
        }
        long start = Long.parseLong(first);
        if (last.isEmpty()) {
            return Optional.of(new ByteRange(start, contentLength - 1));
        }
        long end = Long.parseLong(last);
        return end < start ? Optional.empty()
                : Optional.of(
                        new ByteRange(start, Math.min(end, contentLength - 1)));
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
