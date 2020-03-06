/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.JavaScriptException;

import elemental.client.Browser;

/**
 * Helper class for using window.console. Does not log anything to console if
 * production mode is enabled.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class Console {
    private static boolean shouldLogToBrowserConsole;

    @FunctionalInterface
    // Runnable that can throw
    private interface DeferWithoutEntryTask {
        void run() throws Exception;
    }

    private Console() {
    }

    /**
     * Changes logger behavior, making it skip all browser logging for
     * production mode.
     *
     * @param isProductionMode
     *            if an application is in the production mode or not
     */
    public static void setProductionMode(boolean isProductionMode) {
        shouldLogToBrowserConsole = !isProductionMode;
    }

    /**
     * If not in production mode, logs the given message to the browser console
     * using the debug log level.
     * <p>
     * If used directly in a JVM, writes the message to standard output
     * disregarding of the production mode settings.
     *
     * @param message
     *            the message to log
     */
    public static void debug(Object message) {
        if (GWT.isScript()) {
            if (shouldLogToBrowserConsole) {
                Browser.getWindow().getConsole().debug(message);
            }
        } else {
            System.out.println(message);
        }
    }

    /**
     * If not in production mode, logs the given message to the browser console
     * using the info log level.
     * <p>
     * If used directly in a JVM, writes the message to standard output
     * disregarding of the production mode settings.
     *
     * @param message
     *            the message to log
     */
    public static void log(Object message) {
        if (GWT.isScript()) {
            if (shouldLogToBrowserConsole) {
                Browser.getWindow().getConsole().log(message);
            }
        } else {
            System.out.println(message);
        }
    }

    /**
     * If not in production mode, logs the given message to the browser console
     * using the warning log level.
     * <p>
     * If used directly in a JVM, writes the message to standard error
     * disregarding of the production mode settings.
     *
     * @param message
     *            the message to log
     */
    public static void warn(Object message) {
        if (GWT.isScript()) {
            if (shouldLogToBrowserConsole) {
                Browser.getWindow().getConsole().warn(message);
            }
        } else {
            System.err.println(message);
        }
    }

    /**
     * If not in production mode, logs the given message to the browser console
     * using the error log level.
     * <p>
     * If used directly in a JVM, writes the message to standard error
     * disregarding of the production mode settings.
     *
     * @param message
     *            the message to log
     */
    public static void error(Object message) {
        if (GWT.isScript()) {
            if (shouldLogToBrowserConsole) {
                Browser.getWindow().getConsole().error(message);
            }
        } else {
            System.err.println(message);
        }
    }

    /**
     * Logs the stacktrace of an exception to the browser console. Logging is
     * done asynchronously since that approach allows reporting it with highest
     * possible fidelity.
     *
     * @param exception
     *            the exception for which
     */
    public static void reportStacktrace(Exception exception) {
        if (GWT.isScript()) {
            if (shouldLogToBrowserConsole) {
                doReportStacktrace(exception);
            }
        } else {
            exception.printStackTrace();
        }
    }

    private static void doReportStacktrace(Exception exception) {
        // Defer without $entry to bypass some of GWT's exception handling
        deferWithoutEntry(() -> {
            // Bypass regular exception reporting
            UncaughtExceptionHandler originalHandler = GWT
                    .getUncaughtExceptionHandler();
            GWT.setUncaughtExceptionHandler(
                    ignore -> GWT.setUncaughtExceptionHandler(originalHandler));

            // Throw in the appropriate way
            if (exception instanceof JavaScriptException) {
                // Throw originally thrown instance through JS
                jsThrow(((JavaScriptException) exception).getThrown());
            } else {
                throw exception;
            }
        });
    }

    private static native void jsThrow(Object exception)
    /*-{
      throw exception;
    }-*/;

    private static native void deferWithoutEntry(DeferWithoutEntryTask task)
    /*-{
      $wnd.setTimeout(function() {
        task.@DeferWithoutEntryTask::run()();
      }, 0);
    }-*/;

}
