/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server.connect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Class that contains all Vaadin Connect customizable properties.
 */
@Component
@ConfigurationProperties("vaadin.connect")
public class VaadinConnectProperties {

    @Value("${vaadin.connect.endpoint:/connect}")
    private String vaadinConnectEndpoint;

    @Value("${vaadin.connect.auth.token-signing-key:}")
    private String vaadinConnectTokenSigningKey;

    /**
     * Customize the endpoint for all Vaadin Connect exports. See default value
     * in the {@link VaadinConnectProperties#vaadinConnectEndpoint} field
     * annotation.
     *
     * @return endpoint that should be used to access any Vaadin Connect export
     */
    public String getVaadinConnectEndpoint() {
        return vaadinConnectEndpoint;
    }

    /**
     * Customize the application token signing key. When not given any key, the
     * application will use a random one which is generated each time the
     * application is run.
     *
     * @return token signing key
     */
    public String getVaadinConnectTokenSigningKey() {
        return vaadinConnectTokenSigningKey;
    }
}
