/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.provider.BackendDataProviderTest.StrBeanBackEndDataProvider;
import com.vaadin.flow.function.SerializablePredicate;

public class ConfigurableFilterDataProviderWrapperTest {
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
    public void void_setFilter() {
        configurableVoid.setFilter(xyzFilter);

        Assert.assertEquals("Set filter should be used", 1,
                configurableVoid.size(new Query<>()));

        configurableVoid.setFilter(null);

        Assert.assertEquals("null filter should return all items", 100,
                configurableVoid.size(new Query<>()));
    }

    @Test(expected = Exception.class)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void void_nonNullQueryFilter_throws() {
        configurableVoid
                .size((Query) new Query<StrBean, String>("invalid filter"));
    }

    @Test
    public void predicate_setFilter() {
        configurablePredicate.setFilter(Integer.valueOf(50));

        Assert.assertEquals("Set filter should be used", 49,
                configurablePredicate.size(new Query<>()));

        configurablePredicate.setFilter(null);

        Assert.assertEquals("null filter should return all items", 100,
                configurablePredicate.size(new Query<>()));
    }

    @Test
    public void predicate_queryFilter() {
        Assert.assertEquals("Query filter should be used", 1,
                configurablePredicate.size(new Query<>("Xyz")));

        Assert.assertEquals("null query filter should return all items", 100,
                configurablePredicate.size(new Query<>()));
    }

    @Test
    public void predicate_combinedFilters() {
        configurablePredicate.setFilter(Integer.valueOf(50));

        Assert.assertEquals("Both filters should be used", 0,
                configurablePredicate.size(new Query<>("Xyz")));

        configurablePredicate.setFilter(null);

        Assert.assertEquals("Only zyz filter should be used", 1,
                configurablePredicate.size(new Query<>("Xyz")));
    }

}
