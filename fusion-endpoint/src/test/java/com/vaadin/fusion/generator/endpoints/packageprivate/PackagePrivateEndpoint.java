/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.packageprivate;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.Endpoint;

/**
 * PackagePrivateEndpoint, created on 03/12/2020 18.22
 * 
 * @author nikolaigorokhov
 */
@Endpoint
@AnonymousAllowed
class PackagePrivateEndpoint {

    public PackagePrivateEndpoint() {
    }

    public String getRequest() {
        return "Hello";
    }
}
