/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.superclassmethods;

import com.vaadin.fusion.Endpoint;
import com.vaadin.fusion.generator.endpoints.superclassmethods.PersonEndpoint.Person;

@Endpoint
public class PersonEndpoint extends CrudEndpoint<Person, Integer>
        implements PagedData<Person> {

    public static class Person {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
