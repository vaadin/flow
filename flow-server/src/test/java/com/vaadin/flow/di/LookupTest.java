/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.di;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class LookupTest {

    @Test(expected = NullPointerException.class)
    public void of_nullServiceObject_throws() {
        Lookup.of(null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = IllegalArgumentException.class)
    public void of_serviceNotExtendingType_throws() {
        String service = "";
        Class type = List.class;
        Lookup.of(service, type);
    }

    @Test
    public void of_serviceIsFoundByProvidedTypes_serviceIsNotFoundByNotProvidedTypes() {
        ArrayList<String> service = new ArrayList<String>();
        Lookup lookup = Lookup.of(service, Collection.class);
        Assert.assertEquals(service, lookup.lookup(Collection.class));
        Assert.assertEquals(1, lookup.lookupAll(Collection.class).size());
        Assert.assertEquals(service,
                lookup.lookupAll(Collection.class).iterator().next());
        Assert.assertNull(lookup.lookup(List.class));
        Assert.assertEquals(0, lookup.lookupAll(List.class).size());
    }

    @Test
    public void compose_bothLookupsHasService_resultingLookupReturnsServiceFromFirstLookup() {
        ArrayList<String> service = new ArrayList<String>();
        Lookup lookup1 = Lookup.of(service, Collection.class);
        Lookup lookup2 = Lookup.of(new LinkedList<String>(), Collection.class);

        Lookup compose = Lookup.compose(lookup1, lookup2);
        Assert.assertSame(service, compose.lookup(Collection.class));
    }

    @Test
    public void compose_firstLookupHasNoService_resultingLookupReturnsServiceFromSecondLookup() {
        Lookup lookup1 = Lookup.of(new LinkedList<String>(), List.class);
        ArrayList<String> service = new ArrayList<String>();
        Lookup lookup2 = Lookup.of(service, Collection.class);

        Lookup compose = Lookup.compose(lookup1, lookup2);
        Assert.assertSame(service, compose.lookup(Collection.class));
    }

    @Test
    public void compose_differentServicesForSameType_resultingLookupAllReturnsAllServices() {
        LinkedList<String> service1 = new LinkedList<String>();
        Lookup lookup1 = Lookup.of(service1, List.class);
        ArrayList<String> service2 = new ArrayList<String>();
        Lookup lookup2 = Lookup.of(service2, List.class, Collection.class);

        Lookup compose = Lookup.compose(lookup1, lookup2);
        @SuppressWarnings("rawtypes")
        Collection<List> lookupAll = compose.lookupAll(List.class);
        Assert.assertEquals(2, lookupAll.size());
        Assert.assertTrue(lookupAll.contains(service1));
        Assert.assertTrue(lookupAll.contains(service2));
    }
}
