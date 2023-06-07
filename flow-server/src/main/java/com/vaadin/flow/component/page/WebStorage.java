package com.vaadin.flow.component.page;

import com.vaadin.flow.component.UI;

/**
 * Wrapper for similarly named Browser API. WebStorage may be handy to save some
 * data that you want to be stored on the client side, instead of e.g. database
 * on the server. An example could be certain UI settings that the same users
 * might want to have set differently based on their device.
 */
public class WebStorage {

    public enum Storage {
        /**
         * Web storage saved in the browser "permanently".
         */
        localStorage,
        /**
         * Web storage saved in the browser until the browser is closed
         */
        sessionStorage
    }

    public interface Callback {
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
        setItem(Storage.localStorage, key, value);
    }

    /**
     * Sets given key-value pair to give storage type
     *
     * @param storage
     *            the storage type
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setItem(Storage storage, String key, String value) {
        setItem(UI.getCurrent(), storage, key, value);
    }

    /**
     * Sets given key-value pair to give storage type
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
        ui.getPage().executeJs("window[$0].setItem($1,$2)", storage.toString(),
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
        removeItem(Storage.localStorage, key);
    }

    /**
     * Removes the value associated by the given key from the
     * Storage.localStorage
     *
     * @param storage
     *            the storage type from which the value will be removed
     * @param key
     *            the key to be deleted
     */
    public static void removeItem(Storage storage, String key) {
        removeItem(UI.getCurrent(), storage, key);
    }

    /**
     * Removes the value associated by the given key from the
     * Storage.localStorage
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage type from which the value will be removed
     * @param key
     *            the key to be deleted
     */
    public static void removeItem(UI ui, Storage storage, String key) {
        ui.getPage().executeJs("window[$0].removeItem($1)", storage.toString(),
                key);
    }

    /**
     * Clears all values from the Storage.localStorage
     */
    public static void clear() {
        clear(Storage.localStorage);
    }

    /**
     * Clears the given storage.
     *
     * @param storage
     *            the storage
     */
    public static void clear(Storage storage) {
        clear(UI.getCurrent(), storage);
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
        ui.getPage().executeJs("window[$0].clear()", storage.toString());
    }

    /**
     * Asynchronously gets an item from the Storage.localStorage
     *
     * @param key
     *            the key for which the value will be fetched
     * @param callback
     *            the callback that gets the value once transferred from the
     *            client side
     */
    public static void getItem(String key, Callback callback) {
        getItem(Storage.localStorage, key, callback);
    }

    /**
     * Asynchronously gets an item from the given storage
     *
     * @param storage
     *            the storage
     * @param key
     *            the key for which the value will be fetched
     * @param callback
     *            the callback that gets the value once transferred from the
     *            client side
     */
    public static void getItem(Storage storage, String key, Callback callback) {
        getItem(UI.getCurrent(), storage, key, callback);
    }

    /**
     * Asynchronously gets an item from the given storage
     *
     * @param ui
     *            the UI for which the storage is related to
     * @param storage
     *            the storage
     * @param key
     *            the key for which the value will be fetched
     * @param callback
     *            the callback that gets the value once transferred from the
     *            client side
     */
    public static void getItem(UI ui, Storage storage, String key,
            Callback callback) {
        ui.getPage()
                .executeJs("return window[$0].getItem($1);", storage.toString(),
                        key)
                .then(String.class, callback::onValueDetected, s -> {
                    // for error (most likely non-existing mapping), return null
                    callback.onValueDetected(null);
                });
    }

}
