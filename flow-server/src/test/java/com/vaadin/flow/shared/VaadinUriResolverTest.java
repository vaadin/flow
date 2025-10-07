/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.shared;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VaadinUriResolverTest {

    private final class NullContextVaadinUriResolver extends VaadinUriResolver {
        public String resolveVaadinUri(String uri) {
            return super.resolveVaadinUri(uri, "http://someplace/");
        }
    }

    @Test
    public void testContextProtocol() {
        NullContextVaadinUriResolver resolver = new NullContextVaadinUriResolver();
        assertEquals("http://someplace/my-component.html",
                resolver.resolveVaadinUri("context://my-component.html"));
    }

}
