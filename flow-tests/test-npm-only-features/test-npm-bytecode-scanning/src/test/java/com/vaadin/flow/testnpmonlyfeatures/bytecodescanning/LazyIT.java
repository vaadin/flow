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
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class LazyIT extends ChromeBrowserTest {

    @Test
    public void lazyLoadedWhenEnteringLazyView() {
        open();

        // The component should not be loaded yet
        TestBenchElement component = $("lazy-component").first();
        Assert.assertEquals("", component.getText());

        String lazyView = getTestURL(getRootURL(),
                "/view/com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.LazyView",
                null);

        getDriver().get(lazyView);
        // The component should now be loaded
        component = $("lazy-component").first();
        Assert.assertEquals("Lazy component", component.getText());
    }

    @Override
    protected Class<? extends Component> getViewClass() {
        return EagerViewWithLazyComponent.class;
    }

}
