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
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SpringBootOnly.class)
public class ResourcesIT extends AbstractSpringTest {

    private String loadFile(String file) {
        getDriver().get(getContextRootURL() + file);
        return $("body").first().getPropertyString("textContent")
                .replaceFirst("\n$", "");
    }

    @Test
    public void resourceInPublic() {
        Assert.assertEquals("This is in the public folder on the classpath",
                loadFile("/public-file.txt"));
    }

    @Test
    public void resourceInStatic() {
        Assert.assertEquals("This is in the static folder on the classpath",
                loadFile("/static-file.txt"));
    }

    @Test
    public void resourceInResources() {
        Assert.assertEquals("This is in the resources folder on the classpath",
                loadFile("/resources-file.txt"));
    }

    @Test
    public void resourceInMetaInfResources() {
        Assert.assertEquals(
                "This is in the META-INF/resources folder on the classpath",
                loadFile("/metainfresources-file.txt"));
    }
}
