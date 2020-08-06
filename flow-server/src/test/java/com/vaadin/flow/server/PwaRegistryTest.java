/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server;

import javax.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

@PWA(name = "foo", shortName = "bar")
public class PwaRegistryTest {

    @Test
    public void pwaIconIsGeneratedBasedOnClasspathIcon_servletContextHasNoResources()
            throws IOException {
        ServletContext context = Mockito.mock(ServletContext.class);
        // PWA annotation has default value for "iconPath" but servlet context
        // has no resource for that path, in that case the ClassPath URL will be
        // checked which is "META-INF/resources/icons/icon.png" (this path
        // available is in the test resources folder). The icon in this path
        // differs from the default icon and set of icons will be generated
        // based on it
        PwaRegistry registry = new PwaRegistry(
                PwaRegistryTest.class.getAnnotation(PWA.class), context);
        List<PwaIcon> icons = registry.getIcons();
        // This icon has width 32 and it's generated based on a custom icon (see
        // above)
        PwaIcon pwaIcon = icons.stream().filter(icon -> icon.getWidth() == 32)
                .findFirst().get();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pwaIcon.write(stream);
        // the default image has 47 on the position 36
        Assert.assertEquals(26, stream.toByteArray()[36]);
    }
}
