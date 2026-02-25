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
package com.vaadin.flow.data.provider;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.provider.BackendDataProviderTest.StrBeanBackEndDataProvider;
import com.vaadin.flow.function.SerializablePredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurableFilterDataProviderWrapperTest {
    private static SerializablePredicate<StrBean> xyzFilter = item -> item
            .getValue().equals("Xyz");

    private StrBeanBackEndDataProvider backEndProvider = new StrBeanBackEndDataProvider(
            StrBean.generateRandomBeans(100));
    private ConfigurableFilterDataProvider<StrBean, Void, SerializablePredicate<StrBean>> configurableVoid = backEndProvider
            .withConfigurableFilter();
    private ConfigurableFilterDataProvider<StrBean, String, Integer> configurablePredicate = backEndProvider
            .withConfigurableFilter((queryFilter, configuredFilter) -> item -> {
                if (queryFilter != null
                        && !item.getValue().equals(queryFilter)) {
                    return false;
                }

                if (configuredFilter != null
                        && item.getId() < configuredFilter.intValue()) {
                    return false;
                }

                return true;
            });

    @Test
    void void_setFilter() {
        configurableVoid.setFilter(xyzFilter);

        assertEquals(1, configurableVoid.size(new Query<>()),
                "Set filter should be used");

        configurableVoid.setFilter(null);

        assertEquals(100, configurableVoid.size(new Query<>()),
                "null filter should return all items");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void void_nonNullQueryFilter_throws() {
        assertThrows(Exception.class, () -> configurableVoid
                .size((Query) new Query<StrBean, String>("invalid filter")));
    }

    @Test
    void predicate_setFilter() {
        configurablePredicate.setFilter(Integer.valueOf(50));

        assertEquals(49, configurablePredicate.size(new Query<>()),
                "Set filter should be used");

        configurablePredicate.setFilter(null);

        assertEquals(100, configurablePredicate.size(new Query<>()),
                "null filter should return all items");
    }

    @Test
    void predicate_queryFilter() {
        assertEquals(1, configurablePredicate.size(new Query<>("Xyz")),
                "Query filter should be used");

        assertEquals(100, configurablePredicate.size(new Query<>()),
                "null query filter should return all items");
    }

    @Test
    void predicate_combinedFilters() {
        configurablePredicate.setFilter(Integer.valueOf(50));

        assertEquals(0, configurablePredicate.size(new Query<>("Xyz")),
                "Both filters should be used");

        configurablePredicate.setFilter(null);

        assertEquals(1, configurablePredicate.size(new Query<>("Xyz")),
                "Only zyz filter should be used");
    }

}
