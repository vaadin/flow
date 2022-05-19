/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "server.port = 1234",
        "vaadin.url-mapping=/ui/*" })
public class DevModeBrowserLauncherVaadinAndServletMappingTest
        extends AbstractDevModeBrowserLauncherTest {

    @Test
    public void getUrl_withContextPathAndUrlMapping_givesUrlWithContextPathAndUrlMapping() {
        MockServletContext ctx = (MockServletContext) app.getServletContext();
        ctx.setContextPath("/contextpath");
        String url = DevModeBrowserLauncher.getUrl(app);
        Assert.assertEquals("http://localhost:1234/contextpath/ui/", url);
    }

}
