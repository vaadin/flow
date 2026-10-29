/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend.installer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyConfigTest {

    @Test
    void proxyToString_withAuthentication_hidesCredentials() {
        String proxy = new ProxyConfig.Proxy("my-proxy", "http",
                "proxy.example.com", 8080, "john", "s3cret", null).toString();

        assertTrue(proxy.contains("with username/password authentication"),
                proxy);
        assertFalse(proxy.contains("john"), proxy);
        assertFalse(proxy.contains("s3cret"), proxy);
    }
}
