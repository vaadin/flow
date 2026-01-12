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
package com.vaadin.flow.ccdmtest;

import org.junit.Test;
import org.openqa.selenium.By;

public class ServerSideNavigationExceptionHandlingIT extends CCDMTest {

    @Test
    public void should_showErrorView_when_targetViewThrowsException() {
        openVaadinRouter();

        findAnchor("view-with-server-view-button").click();

        // Navigate to a server-side view that throws exception.
        findElement(By.id("serverViewThrowsExcpetionButton")).click();

        assertView("errorView",
                "Tried to navigate to a view without being authenticated",
                "view-throws-exception");
    }

}
