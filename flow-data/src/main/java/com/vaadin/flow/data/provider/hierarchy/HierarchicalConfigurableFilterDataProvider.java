/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.data.provider.hierarchy;

import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;

/**
 * A hierarchical data provider that supports programmatically setting a filter
 * that will be applied to all queries.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the data provider item type
 * @param <Q>
 *            the query filter type
 * @param <C>
 *            the configurable filter type
 *
 * @see ConfigurableFilterDataProvider
 */
public interface HierarchicalConfigurableFilterDataProvider<T, Q, C>
        extends ConfigurableFilterDataProvider<T, Q, C>,
        HierarchicalDataProvider<T, Q> {

}
