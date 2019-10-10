/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class FallbackByteCodeScanningIT extends ChromeBrowserTest {

    @Test
    public void buttonIsInitializedInProductionMode() {
        open();

        TestBenchElement component = $(TestBenchElement.class)
                .id(ByteCodeScanningView.COMPONENT_ID);

        // in production mode with fallback chunk component should be
        // initialized
        Assert.assertFalse("component expected initialized in production mode",
                component.$("button").all().isEmpty());
    }

    @Override
    protected Class<? extends Component> getViewClass() {
        return ByteCodeScanningView.class;
    }

}
