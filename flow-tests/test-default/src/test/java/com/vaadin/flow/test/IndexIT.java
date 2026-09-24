/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test;

import java.util.List;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.UnorderedListElement;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(IndexView.class)
public class IndexIT extends AbstractDefaultIT {

    @BrowserTest
    public void indexListsRoutes_linkNavigatesToView() {
        open();

        UnorderedListElement routes = $(UnorderedListElement.class)
                .id(IndexView.ROUTES_ID);
        List<String> hrefs = routes.$(AnchorElement.class).all().stream()
                .map(link -> link.getDomAttribute("href")).toList();
        assertTrue(hrefs.contains("info"),
                "Link to InfoView missing, found: " + hrefs);
        assertFalse(hrefs.contains(""), "Index view should not link to itself");

        routes.$(AnchorElement.class).attribute("href", "info").first().click();
        waitUntil(driver -> driver.getCurrentUrl()
                .equals(getRootURL() + "/info"));
        assertTrue(
                $("div").attributeContains("class", "infoContainer").exists());
    }
}
