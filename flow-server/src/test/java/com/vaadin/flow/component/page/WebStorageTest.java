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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage.Storage;
import com.vaadin.flow.js.JsCall;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The storage is named on the wire rather than in the JavaScript, so the
 * arguments of a call are what decides which storage is worked on and with
 * what. A swapped or dropped one would only show in a browser, which is what
 * these pin.
 */
class WebStorageTest {

    @Test
    void setItem_namesTheStorageKeyAndValue() {
        assertEquals(
                new JsCall(WebStorage.WebStorageJs.class, "setItem",
                        List.of("sessionStorage", "key", "value")),
                onlyCallOf(ui -> WebStorage.setItem(ui, Storage.SESSION_STORAGE,
                        "key", "value")));
    }

    @Test
    void removeItem_namesTheStorageAndKey() {
        assertEquals(
                new JsCall(WebStorage.WebStorageJs.class, "removeItem",
                        List.of("localStorage", "key")),
                onlyCallOf(ui -> WebStorage.removeItem(ui,
                        Storage.LOCAL_STORAGE, "key")));
    }

    @Test
    void clear_namesTheStorage() {
        assertEquals(
                new JsCall(WebStorage.WebStorageJs.class, "clear",
                        List.of("localStorage")),
                onlyCallOf(ui -> WebStorage.clear(ui, Storage.LOCAL_STORAGE)));
    }

    @Test
    void getItem_namesTheStorageAndKey() {
        assertEquals(
                new JsCall(WebStorage.WebStorageJs.class, "getItem",
                        List.of("sessionStorage", "key")),
                onlyCallOf(ui -> WebStorage.getItem(ui, Storage.SESSION_STORAGE,
                        "key", value -> {
                        })));
    }

    /**
     * The only call of declared JavaScript that the given operation scheduled.
     */
    private static JsCall onlyCallOf(SerializableUiOperation operation) {
        MockUI ui = MockUI.createUI();
        operation.run(ui);
        return ui.onlyScheduledJsCall();
    }

    @FunctionalInterface
    private interface SerializableUiOperation {
        void run(UI ui);
    }
}
