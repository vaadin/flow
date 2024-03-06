/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Backend implements Serializable {

    public static List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        clients.add(new Client("Joe", "Mallone"));
        clients.add(new Client("Janine", "Mallone"));
        clients.add(new Client("Peter", "Parker"));
        clients.add(new Client("Tony", "Stark"));
        clients.add(new Client("Bruce", "Banner"));

        return clients;
    }
}
