/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.modelpackage;

import com.vaadin.fusion.Endpoint;
import com.vaadin.fusion.generator.endpoints.modelpackage.subpackage.Account;

@Endpoint
public class SubModelPackageEndpoint {

    public Account getSubAccountPackage(String name) {
        return new Account();
    }

}
