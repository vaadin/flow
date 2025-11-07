/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.PrintWriter;
import java.io.Serializable;

import org.apache.commons.io.output.StringBuilderWriter;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class MockLogger implements Logger, Serializable {

    StringBuilder logs = new StringBuilder();
    public boolean includeStackTrace = false;

    public static String TRACE = "[Trace] ";
    public static String DEBUG = "[Debug] ";
    public static String WARN = "[Warning] ";
    public static String ERROR = "[Error] ";
    public static String INFO = "[Info] ";

    public String getLogs() {
        return logs.toString().replaceAll("\r", "");
    }

    private void append(Throwable throwable) {
        if (throwable != null && includeStackTrace) {
            throwable.printStackTrace(
                    new PrintWriter(new StringBuilderWriter(logs)));
        }
    }

    public void clearLogs() {
        logs = new StringBuilder();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String s) {
        logs.append(TRACE).append(s).append("\n");
    }

    @Override
    public void trace(String s, Object o) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(TRACE).append(s).append("\n");

    }

    @Override
    public void trace(String s, Object o, Object o1) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        s = s.replaceFirst("\\{\\}",
                o1.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(TRACE).append(s).append("\n");

    }

    @Override
    public void trace(String s, Object... objects) {
        logs.append(TRACE);
        for (Object object : objects) {
            s = s.replaceFirst("\\{\\}",
                    object.toString().replaceAll("\\\\", "\\\\\\\\"));
        }
        logs.append(s).append("\n");
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logs.append(TRACE).append(s).append("\n");
        append(throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public void trace(Marker marker, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String s) {
        logs.append(DEBUG).append(s).append("\n");
    }

    @Override
    public void debug(String s, Object o) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(DEBUG).append(s).append("\n");
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        s = s.replaceFirst("\\{\\}",
                o1.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(DEBUG).append(s).append("\n");
    }

    @Override
    public void debug(String s, Object... objects) {
        logs.append(DEBUG);
        for (Object object : objects) {
            s = s.replaceFirst("\\{\\}",
                    object.toString().replaceAll("\\\\", "\\\\\\\\"));
        }
        logs.append(s).append("\n");
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logs.append(DEBUG).append(s).append("\n");
        append(throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public void debug(Marker marker, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String s) {
        logs.append(INFO).append(s).append("\n");
    }

    @Override
    public void info(String s, Object o) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(INFO).append(s).append("\n");
    }

    @Override
    public void info(String s, Object o, Object o1) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        s = s.replaceFirst("\\{\\}",
                o1.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(INFO).append(s).append("\n");
    }

    @Override
    public void info(String s, Object... objects) {
        logs.append(INFO);
        for (Object object : objects) {
            s = s.replaceFirst("\\{\\}",
                    object.toString().replaceAll("\\\\", "\\\\\\\\"));
        }
        logs.append(s).append("\n");
    }

    @Override
    public void info(String s, Throwable throwable) {
        logs.append(INFO).append(s).append("\n");
        append(throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public void info(Marker marker, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String s) {
        logs.append(WARN).append(s).append("\n");
    }

    @Override
    public void warn(String s, Object o) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(WARN).append(s).append("\n");
    }

    @Override
    public void warn(String s, Object... objects) {
        logs.append(WARN);
        for (Object object : objects) {
            s = s.replaceFirst("\\{\\}",
                    object.toString().replaceAll("\\\\", "\\\\\\\\"));
        }
        logs.append(s).append("\n");
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        s = s.replaceFirst("\\{\\}",
                o1.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(WARN).append(s).append("\n");
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logs.append(WARN).append(s).append("\n");
        append(throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String s) {
        logs.append(ERROR).append(s).append("\n");
    }

    @Override
    public void error(String s, Object o) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(ERROR).append(s).append("\n");
    }

    @Override
    public void error(String s, Object o, Object o1) {
        s = s.replaceFirst("\\{\\}",
                o.toString().replaceAll("\\\\", "\\\\\\\\"));
        s = s.replaceFirst("\\{\\}",
                o1.toString().replaceAll("\\\\", "\\\\\\\\"));
        logs.append(ERROR).append(s).append("\n");
    }

    @Override
    public void error(String s, Object... objects) {
        logs.append(ERROR);
        for (Object object : objects) {
            s = s.replaceFirst("\\{\\}",
                    object.toString().replaceAll("\\\\", "\\\\\\\\"));
        }
        logs.append(s).append("\n");
    }

    @Override
    public void error(String s, Throwable throwable) {
        logs.append(ERROR).append(s).append("\n");
        append(throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        throw new UnsupportedOperationException();
    }
}
