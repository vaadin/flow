/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.collectionendpoint;

import java.util.ArrayList;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class CollectionEndpoint {

    /**
     * Get a collection by author name. The generator should not mix this type
     * with the Java's Collection type.
     *
     * @param name
     *            author name
     * @return a collection
     */
    public Collection getCollectionByAuthor(String name) {
        return new Collection();
    }

    /**
     * Get a list of user name.
     *
     * @return list of user name
     */
    public java.util.Collection<String> getListOfUserName() {
        return new ArrayList<>();
    }

    public static class Collection {
        private String collectionName;
        private String type;
        private String author;
    }

}