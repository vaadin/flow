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

import com.vaadin.flow.component.html.testbench.LabelElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.ElementQuery;
import com.vaadin.testbench.TestBenchElement;


public class ByteCodeScanningIT extends ChromeBrowserTest {

    @Test
    public void buttonIsShownIffInDevMode() {
        open();

        LabelElement modeLabel = $(LabelElement.class).id(ByteCodeScanningView.MODE_LABEL_ID);

        ElementQuery<TestBenchElement> component = $(TestBenchElement.class)
                .attribute("id", ByteCodeScanningView.COMPONENT_ID);
        ElementQuery<LabelElement> componentMissing = $(LabelElement.class)
                .attribute("id", ByteCodeScanningView.COMPONENT_MISSING_LABEL_ID);
        boolean productionMode = Boolean.parseBoolean(modeLabel.getText());
        if (productionMode) {
            // in production mode we use optimized bundle by default, so component should be missing
            Assert.assertFalse("component expected missing in production mode",
                    component.exists());
            Assert.assertTrue("label about missing component expected present in production mode",
                    componentMissing.exists());
        } else {
            // in dev mode we use complete bundle by default, so component should be present
            Assert.assertTrue("component expected present in dev mode", component.exists());
            Assert.assertFalse("label about missing component expected missing in dev mode",
                    componentMissing.exists());
        }
    }

}
