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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * Wrapper for similarly named Browser API. WebStorage may be handy to save some
 * data that you want to be stored on the client side, instead of e.g. database
 * on the server. An example could be certain UI settings that the same users
 * might want to have set differently based on their device.
 * 
 * @since 24.2
 */
public interface WebStorage extends Serializable {

    public enum Storage {
        /**
         * Web storage saved in the browser "permanently". "localStorage" in the
         * browser APIs.
         */
        LOCAL_STORAGE,
        /**
         * Web storage saved in the browser until the browser is closed.
         * "sessionStorage" in the browser APIs.
         */
        SESSION_STORAGE;

        @Override
        public String toString() {
            if (LOCAL_STORAGE == this) {
                return "localStorage";
            } else {
                return "sessionStorage";
            }
        }
    }

    /**
     * This callback is notified after the value has been retrieved from the
     * client side.
     */
    @FunctionalInterface
    public interface Callback extends Serializable {
        /**
         * This method is called when the value detection is complete.
         *
         * @param value
         *            the value or null if the value was not available.
         */
        void onValueDetected(String value);
    }

    /**
     * Sets given key-value pair to Storage.localStorage
     *
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setItem(String key, String value) {
        setItem(Storage.LOCAL_STORAGE, key, value);
    }

    /**
     * Sets given key-value pair to given storage type
     *
     * @param storage
     *            the storage type
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setItem(Storage storage, String key, String value) {
        setItem(UI.getCurrentOrThrow(), storage, key, value);
    }

    /**
     * Sets given key-value pair to given storage type
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage type
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setItem(UI ui, Storage storage, String key,
            String value) {
        ui.getPage().executeJs(WebStorageJs.class).setItem(storage.toString(),
                key, value);
    }

    /**
     * Removes the value associated by the given key from the
     * Storage.localStorage
     *
     * @param key
     *            the key to be deleted
     */
    public static void removeItem(String key) {
        removeItem(Storage.LOCAL_STORAGE, key);
    }

    /**
     * Removes the value associated by the given key from the provided storage
     * type
     *
     * @param storage
     *            the storage type from which the value will be removed
     * @param key
     *            the key to be deleted
     */
    public static void removeItem(Storage storage, String key) {
        removeItem(UI.getCurrentOrThrow(), storage, key);
    }

    /**
     * Removes the value associated by the given key from the provided storage
     * type
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage type from which the value will be removed
     * @param key
     *            the key to be deleted
     */
    public static void removeItem(UI ui, Storage storage, String key) {
        ui.getPage().executeJs(WebStorageJs.class)
                .removeItem(storage.toString(), key);
    }

    /**
     * Clears all values from the Storage.localStorage
     */
    public static void clear() {
        clear(Storage.LOCAL_STORAGE);
    }

    /**
     * Clears the given storage.
     *
     * @param storage
     *            the storage
     */
    public static void clear(Storage storage) {
        clear(UI.getCurrentOrThrow(), storage);
    }

    /**
     * Clears the given storage.
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage
     */
    public static void clear(UI ui, Storage storage) {
        ui.getPage().executeJs(WebStorageJs.class).clear(storage.toString());
    }

    /**
     * Asynchronously gets an item from the local storage.
     *
     * @param key
     *            the key for which the value will be fetched
     * @param callback
     *            the callback that gets the value once transferred from the
     *            client side or <code>null</code> if the value was not
     *            available.
     */
    public static void getItem(String key, Callback callback) {
        getItem(Storage.LOCAL_STORAGE, key, callback);
    }

    /**
     * Asynchronously gets an item from the given storage.
     *
     * @param storage
     *            the storage
     * @param key
     *            the key for which the value will be fetched
     * @param callback
     *            the callback that gets the value once transferred from the
     *            client side or <code>null</code> if the value was not
     *            available.
     */
    public static void getItem(Storage storage, String key, Callback callback) {
        getItem(UI.getCurrentOrThrow(), storage, key, callback);
    }

    /**
     * Asynchronously gets an item from the given storage.
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage
     * @param key
     *            the key for which the value will be fetched
     * @param callback
     *            the callback that gets the value once transferred from the
     *            client side or <code>null</code> if the value was not
     *            available.
     */
    public static void getItem(UI ui, Storage storage, String key,
            Callback callback) {
        requestItem(ui, storage, key).then(String.class,
                callback::onValueDetected, s -> {
                    LoggerFactory.getLogger(WebStorage.class.getName()).debug(
                            "Error while getting value for key '{}' from storage '{}': {}",
                            key, storage, s);
                    // fallback to null if there was an error
                    callback.onValueDetected(null);
                });
    }

    /**
     * Asynchronously gets an item from the local storage.
     * <p>
     * It is not possible to synchronously wait for the result of the execution
     * while holding the session lock since the request handling thread that
     * makes the result available will also need to lock the session. <br>
     * See {@link PendingJavaScriptResult#toCompletableFuture} for more
     * information.
     *
     * @param key
     *            the key for which the value will be fetched
     * @return a CompletableFuture that will be completed with the value once
     *         transferred from the client side or <code>null</code> if the
     *         value was not available.
     * @since 24.5
     */
    public static CompletableFuture<String> getItem(String key) {
        return getItem(Storage.LOCAL_STORAGE, key);
    }

    /**
     * Asynchronously gets an item from the given storage.
     * <p>
     * It is not possible to synchronously wait for the result of the execution
     * while holding the session lock since the request handling thread that
     * makes the result available will also need to lock the session. <br>
     * See {@link PendingJavaScriptResult#toCompletableFuture} for more
     * information.
     *
     * @param storage
     *            the storage
     * @param key
     *            the key for which the value will be fetched
     * @return a CompletableFuture that will be completed with the value once
     *         transferred from the client side or <code>null</code> if the
     *         value was not available.
     * @since 24.5
     */
    public static CompletableFuture<String> getItem(Storage storage,
            String key) {
        return getItem(UI.getCurrentOrThrow(), storage, key);
    }

    /**
     * Asynchronously gets an item from the given storage.
     * <p>
     * It is not possible to synchronously wait for the result of the execution
     * while holding the session lock since the request handling thread that
     * makes the result available will also need to lock the session. <br>
     * See {@link PendingJavaScriptResult#toCompletableFuture} for more
     * information.
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage
     * @param key
     *            the key for which the value will be fetched
     * @return a CompletableFuture that will be completed with the value once
     *         transferred from the client side or <code>null</code> if the
     *         value was not available.
     * @since 24.5
     */
    public static CompletableFuture<String> getItem(UI ui, Storage storage,
            String key) {
        return requestItem(ui, storage, key).toCompletableFuture(String.class);
    }

    private static PendingJavaScriptResult requestItem(UI ui, Storage storage,
            String key) {
        return ui.getPage().executeJs(WebStorageJs.class)
                .getItem(storage.toString(), key);
    }

    /**
     * What web storage asks of the browser, as a JavaScript definition for
     * {@link Page#executeJs(Class)}.
     * <p>
     * Which storage is worked on is a parameter rather than a declaration of
     * its own, so the bundle carries one function per operation.
     * 
     * @since 25.4
     */
    @JsDefinition
    public interface WebStorageJs extends Serializable {

        /**
         * Writes a value.
         *
         * @param storage
         *            the name of the storage on the window
         * @param key
         *            the key to write under
         * @param value
         *            the value to write
         */
        @JsExpression("window[$0].setItem($1,$2)")
        void setItem(String storage, String key, String value);

        /**
         * Removes the value of a key.
         *
         * @param storage
         *            the name of the storage on the window
         * @param key
         *            the key to remove
         */
        @JsExpression("window[$0].removeItem($1)")
        void removeItem(String storage, String key);

        /**
         * Removes every value of a storage.
         *
         * @param storage
         *            the name of the storage on the window
         */
        @JsExpression("window[$0].clear()")
        void clear(String storage);

        /**
         * Reads the value of a key.
         *
         * @param storage
         *            the name of the storage on the window
         * @param key
         *            the key to read
         * @return the pending result, which answers with the value or with
         *         <code>null</code> when the key has none
         */
        @JsExpression("return window[$0].getItem($1);")
        PendingJavaScriptResult getItem(String storage, String key);
    }

}
