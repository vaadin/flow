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
 *
 */

package com.vaadin.flow.component;

import java.lang.annotation.Annotation;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

public class NpmPackageTest {

    // Keep this here just to make sure the @NpmPackage annotation exists, since
    // it's used outside of the flow-server module, and it has no other use
    // inside flow-server, yet.

    @JsModule("test-component")
    @NpmPackage(value = "test-package", version = "0.0.0")
    private static class TestComponent extends Component {
    }

    @Test
    public void testDummy() {
        Annotation[] annotations = TestComponent.class.getAnnotations();

        Assert.assertEquals("NpmPackage is missing", annotations[1].annotationType(), NpmPackage.class);
    }

}
