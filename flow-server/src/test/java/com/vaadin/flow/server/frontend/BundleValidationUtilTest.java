/*
 * Copyright 2000-2023 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.server.frontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.pro.licensechecker.Product;

public class BundleValidationUtilTest {

    @Test
    public void detectsUsedCommercialComponents() {

        // @formatter:off
        String statsJson = "{"
                + " \"cvdlModules\": { "
                + "  \"component\": {"
                + "      \"name\": \"component\","
                + "      \"version\":\"1.2.3\""
                + "  }, "
                + "  \"comm-component\": {"
                + "      \"name\":\"comm-comp\","
                + "      \"version\":\"4.6.5\""
                + "  }"
                + " }"
                + "}";
        // @formatter:on

        final FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Map<String, String> packages = new HashMap<>();
        packages.put("comm-component", "4.6.5");
        packages.put("@vaadin/button", "1.2.1");
        Mockito.when(scanner.getPackages()).thenReturn(packages);

        List<Product> components = BundleValidationUtil
                .collectLicensedProducts(scanner, statsJson);

        Assert.assertEquals("Only comm-component is used.", 1,
                components.size());
        Assert.assertEquals("Name should match cvdl name not key", "comm-comp",
                components.get(0).getName());
        Assert.assertEquals("", "4.6.5", components.get(0).getVersion());
    }
}