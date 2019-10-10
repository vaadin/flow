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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ByteCodeScanningIT extends ChromeBrowserTest {

    @Test
    public void buttonIsNotInitializedInProductionMode() {
        // in case of this URL parameter presence the Fallback chunk data will
        // be removed and the chunk won't be loaded
        open("drop-fallback");

        TestBenchElement component = $(TestBenchElement.class)
                .id(ByteCodeScanningView.COMPONENT_ID);

        // in production mode without fallback chunk we use optimized bundle by
        // default, so component should not be initialized
        Assert.assertTrue(
                "component expected not initialized in production mode",
                component.$("button").all().isEmpty());
    }

}
