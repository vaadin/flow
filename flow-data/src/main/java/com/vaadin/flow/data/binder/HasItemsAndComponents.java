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
package com.vaadin.flow.data.binder;

/**
 * Represents a component that display a collection of items and can have
 * additional components between the items.
 * <p>
 * <em>Note:</em> this interface is gradually replaced by
 * {@link HasItemComponents} in components, so as to replace {@link HasItems}
 * with {@link com.vaadin.flow.data.provider.HasListDataView},
 * {@link com.vaadin.flow.data.provider.HasLazyDataView} or
 * {@link com.vaadin.flow.data.provider.HasDataView}.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the type of the displayed items
 */
public interface HasItemsAndComponents<T>
        extends HasItemComponents<T>, HasItems<T> {
}
