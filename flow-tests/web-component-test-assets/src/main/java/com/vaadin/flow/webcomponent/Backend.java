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
