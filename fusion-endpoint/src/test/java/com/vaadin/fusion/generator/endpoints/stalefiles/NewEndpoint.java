/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.stalefiles;

import com.vaadin.fusion.Endpoint;

/**
 * New endpoint for testing removal of stale files
 */
@Endpoint
public class NewEndpoint {
    /**
     * Gets an account
     */
    public Account getAccount() {
        final Account account = new Account();
        account.name = "test";
        return account;
    }

    /**
     * Account data model
     */
    public class Account {
        public String name;
    }
}
