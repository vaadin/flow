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
 */
package com.vaadin.flow.uitest.ui;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BaseHrefIT extends ChromeBrowserTest {

    @Test
    public void testBaseHref() throws URISyntaxException {
        URI baseUri = new URI(getTestURL());
        String uiUrl = baseUri.toString();
        String expectedUrl = baseUri.resolve("./link").toString();

        getDriver().get(uiUrl);
        Assert.assertEquals(expectedUrl, getLinkHref());

        getDriver().get(uiUrl + "/foo/bar/baz");
        Assert.assertEquals(expectedUrl, getLinkHref());
    }

    private String getLinkHref() {
        return findElement(By.tagName("a")).getAttribute("href");
    }

}
