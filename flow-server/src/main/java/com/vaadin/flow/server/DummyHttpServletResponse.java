/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * This only exists to be able to implement a backwards compatible
 * StaticFileServer.isStaticResourceRequest.
 * 
 * @deprecated do not use
 */
@Deprecated
public class DummyHttpServletResponse implements HttpServletResponse {

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
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
            }

        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(getOutputStream());
    }

    @Override
    public void setCharacterEncoding(String charset) {
        // This response ignores everything
    }

    @Override
    public void setContentLength(int len) {
        // This response ignores everything
    }

    @Override
    public void setContentLengthLong(long len) {
        // This response ignores everything
    }

    @Override
    public void setContentType(String type) {
        // This response ignores everything
    }

    @Override
    public void setBufferSize(int size) {
        // This response ignores everything
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        // This response ignores everything

    }

    @Override
    public void resetBuffer() {
        // This response ignores everything
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        // This response ignores everything
    }

    @Override
    public void setLocale(Locale loc) {
        // This response ignores everything
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public void addCookie(Cookie cookie) {
        // This response ignores everything
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public String encodeUrl(String url) {
        return url;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return url;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        // This response ignores everything
    }

    @Override
    public void sendError(int sc) throws IOException {
        // This response ignores everything
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        // This response ignores everything
    }

    @Override
    public void setDateHeader(String name, long date) {
        // This response ignores everything
    }

    @Override
    public void addDateHeader(String name, long date) {
        // This response ignores everything
    }

    @Override
    public void setHeader(String name, String value) {
        // This response ignores everything
    }

    @Override
    public void addHeader(String name, String value) {
        // This response ignores everything
    }

    @Override
    public void setIntHeader(String name, int value) {
        // This response ignores everything
    }

    @Override
    public void addIntHeader(String name, int value) {
        // This response ignores everything
    }

    @Override
    public void setStatus(int sc) {
        // This response ignores everything
    }

    @Override
    public void setStatus(int sc, String sm) {
        // This response ignores everything
    }

    @Override
    public int getStatus() {
        return SC_OK;
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }

}
