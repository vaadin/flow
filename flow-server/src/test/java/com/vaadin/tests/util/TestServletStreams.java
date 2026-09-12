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
package com.vaadin.tests.util;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Servlet streams that read from and write to plain streams, for tests that
 * need a request or a response to actually transfer content.
 */
public class TestServletStreams {

    private TestServletStreams() {
        // Only static helpers
    }

    /**
     * Creates a servlet input stream that serves the given contents.
     *
     * @param contents
     *            the contents to serve
     * @return a servlet input stream
     */
    public static ServletInputStream inputStream(byte[] contents) {
        return inputStream(contents, () -> {
        });
    }

    /**
     * Creates a servlet input stream that serves the given contents and runs
     * the given callback when it is read from for the first time.
     *
     * @param contents
     *            the contents to serve
     * @param onFirstRead
     *            the callback to run when the stream is first read from
     * @return a servlet input stream
     */
    public static ServletInputStream inputStream(byte[] contents,
            Runnable onFirstRead) {
        InputStream delegate = new ByteArrayInputStream(contents);
        return new ServletInputStream() {
            private boolean firstRead = true;

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                notifyFirstRead();
                return delegate.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                notifyFirstRead();
                return delegate.read(b, off, len);
            }

            private void notifyFirstRead() {
                if (firstRead) {
                    firstRead = false;
                    onFirstRead.run();
                }
            }
        };
    }

    /**
     * Creates a servlet output stream that writes to the given stream.
     *
     * @param delegate
     *            the stream to write to
     * @return a servlet output stream
     */
    public static ServletOutputStream outputStream(OutputStream delegate) {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) throws IOException {
                delegate.write(b);
            }
        };
    }
}
