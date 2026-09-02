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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

/**
 * Wraps the request and response of an {@link ActiveTransfer} so that every
 * stream they hand out stops transferring bytes once the transfer has been
 * terminated.
 * <p>
 * Wrapping preserves the concrete request and response type, since request
 * handling casts a request to {@link jakarta.servlet.http.HttpServletRequest}
 * for multipart uploads and applications may cast to the servlet types as well.
 * A request or response implementation that is not servlet based is therefore
 * left unwrapped, in which case only the transfers that go through
 * {@link com.vaadin.flow.server.communication.TransferUtil} are terminated.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class TerminableStreamsUtil {

    private static final AtomicBoolean unwrappableTypeWarned = new AtomicBoolean();

    private TerminableStreamsUtil() {
        // Only static helpers
    }

    static VaadinRequest wrapRequest(VaadinRequest request,
            ActiveTransfer transfer) {
        if (request instanceof VaadinServletRequest servletRequest) {
            return new TerminableVaadinServletRequest(servletRequest, transfer);
        }
        warnAboutUnwrappable(request);
        return request;
    }

    static VaadinResponse wrapResponse(VaadinResponse response,
            ActiveTransfer transfer) {
        if (response instanceof VaadinServletResponse servletResponse) {
            return new TerminableVaadinServletResponse(servletResponse,
                    transfer);
        }
        warnAboutUnwrappable(response);
        return response;
    }

    /**
     * Warns once per JVM that transfers cannot be terminated for a request or
     * response implementation that is not servlet based.
     */
    private static void warnAboutUnwrappable(Object requestOrResponse) {
        if (unwrappableTypeWarned.compareAndSet(false, true)) {
            LoggerFactory.getLogger(TerminableStreamsUtil.class).warn(
                    "Ongoing upload and download requests cannot be terminated when the session is invalidated, since {} is not based on the servlet API.",
                    requestOrResponse.getClass().getName());
        }
    }

    private static class TerminableVaadinServletRequest
            extends VaadinServletRequest {

        private final ActiveTransfer transfer;

        private TerminableVaadinServletRequest(VaadinServletRequest request,
                ActiveTransfer transfer) {
            super(request, request.getService());
            this.transfer = transfer;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            transfer.checkNotTerminated();
            return new TerminableServletInputStream(super.getInputStream(),
                    transfer);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            transfer.checkNotTerminated();
            return new BufferedReader(
                    new TerminableReader(super.getReader(), transfer));
        }

        @Override
        public Collection<Part> getParts()
                throws IOException, ServletException {
            transfer.checkNotTerminated();
            return super.getParts().stream()
                    .map(part -> (Part) new TerminablePart(part, transfer))
                    .toList();
        }

        @Override
        public Part getPart(String name) throws IOException, ServletException {
            transfer.checkNotTerminated();
            Part part = super.getPart(name);
            return part == null ? null : new TerminablePart(part, transfer);
        }
    }

    private static class TerminableVaadinServletResponse
            extends VaadinServletResponse {

        private final ActiveTransfer transfer;

        private TerminableVaadinServletResponse(VaadinServletResponse response,
                ActiveTransfer transfer) {
            super(response, response.getService());
            this.transfer = transfer;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            transfer.checkNotTerminated();
            return new TerminableServletOutputStream(super.getOutputStream(),
                    transfer);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            transfer.checkNotTerminated();
            return new PrintWriter(
                    new TerminableWriter(super.getWriter(), transfer));
        }
    }

    /**
     * A part whose content stream stops serving bytes once the transfer has
     * been terminated. Everything else is delegated as-is.
     */
    private record TerminablePart(Part delegate,
            ActiveTransfer transfer) implements Part {

        @Override
        public InputStream getInputStream() throws IOException {
            transfer.checkNotTerminated();
            return new TerminableInputStream(delegate.getInputStream(),
                    transfer);
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getSubmittedFileName() {
            return delegate.getSubmittedFileName();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public void write(String fileName) throws IOException {
            transfer.checkNotTerminated();
            delegate.write(fileName);
        }

        @Override
        public void delete() throws IOException {
            delegate.delete();
        }

        @Override
        public String getHeader(String name) {
            return delegate.getHeader(name);
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return delegate.getHeaders(name);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return delegate.getHeaderNames();
        }
    }

    private static class TerminableInputStream extends FilterInputStream {

        private final ActiveTransfer transfer;

        private TerminableInputStream(InputStream delegate,
                ActiveTransfer transfer) {
            super(delegate);
            this.transfer = transfer;
        }

        @Override
        public int read() throws IOException {
            transfer.checkNotTerminated();
            return super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            transfer.checkNotTerminated();
            return super.read(b, off, len);
        }
    }

    /**
     * The servlet flavor of {@link TerminableInputStream}, which reads through
     * that same wrapper so that both kinds of input behave identically.
     */
    private static class TerminableServletInputStream
            extends ServletInputStream {

        private final ServletInputStream delegate;
        private final TerminableInputStream terminableDelegate;

        private TerminableServletInputStream(ServletInputStream delegate,
                ActiveTransfer transfer) {
            this.delegate = delegate;
            terminableDelegate = new TerminableInputStream(delegate, transfer);
        }

        @Override
        public int read() throws IOException {
            return terminableDelegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return terminableDelegate.read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            return terminableDelegate.available();
        }

        @Override
        public void close() throws IOException {
            terminableDelegate.close();
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }

    private static class TerminableServletOutputStream
            extends ServletOutputStream {

        private final ServletOutputStream delegate;
        private final ActiveTransfer transfer;

        private TerminableServletOutputStream(ServletOutputStream delegate,
                ActiveTransfer transfer) {
            this.delegate = delegate;
            this.transfer = transfer;
        }

        @Override
        public void write(int b) throws IOException {
            transfer.checkNotTerminated();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            transfer.checkNotTerminated();
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            transfer.checkNotTerminated();
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
    }

    /**
     * Every write variant is checked, since FilterWriter passes each of them
     * straight to the wrapped writer instead of funneling them through one
     * method.
     */
    private static class TerminableWriter extends FilterWriter {

        private final ActiveTransfer transfer;

        private TerminableWriter(Writer delegate, ActiveTransfer transfer) {
            super(delegate);
            this.transfer = transfer;
        }

        @Override
        public void write(int c) throws IOException {
            transfer.checkNotTerminated();
            super.write(c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            transfer.checkNotTerminated();
            super.write(cbuf, off, len);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            transfer.checkNotTerminated();
            super.write(str, off, len);
        }

        @Override
        public void flush() throws IOException {
            transfer.checkNotTerminated();
            super.flush();
        }
    }

    private static class TerminableReader extends FilterReader {

        private final ActiveTransfer transfer;

        private TerminableReader(Reader delegate, ActiveTransfer transfer) {
            super(delegate);
            this.transfer = transfer;
        }

        @Override
        public int read() throws IOException {
            transfer.checkNotTerminated();
            return super.read();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            transfer.checkNotTerminated();
            return super.read(cbuf, off, len);
        }
    }
}
