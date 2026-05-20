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
package com.vaadin.client.flow.nodefeature;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.reactive.ReactiveValueChangeEvent;

/**
 * Event fired when the structure of a {@link NodeList} changes. Pure
 * {@code @JsType(isNative=true)} binding.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.nodefeature", name = "ListSpliceEvent")
public class ListSpliceEvent extends ReactiveValueChangeEvent {

    public ListSpliceEvent(NodeList source, int index, JsArray<?> remove,
            JsArray<?> add, boolean clear) {
        super(source);
    }

    public native int getIndex();

    public native JsArray<?> getRemove();

    public native JsArray<?> getAdd();

    public native boolean isClear();

    @Override
    public native NodeList getSource();
}
