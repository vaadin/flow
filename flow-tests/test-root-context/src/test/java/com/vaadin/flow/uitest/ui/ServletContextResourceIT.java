/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.MatcherAssert.assertThat;

public class ServletContextResourceIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view/" + getPath();
    }

    @Test
    public void classResourceIsNotAvailable() throws IOException {
        URL url = new URL(getRootURL() + "/view/" + getPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (HttpURLConnection.HTTP_NOT_FOUND != connection.getResponseCode()) {
            open();

            WebElement body = findElement(By.tagName("body"));
            assertThat(body.getText().trim(), CoreMatchers
                    .startsWith("Could not navigate to '" + getPath() + "'"));
        }
    }

    private String getPath() {
        Class<?> clazz = BaseHrefView.class;
        return clazz.getPackage().getName().replace('.', '/') + "/"
                + clazz.getSimpleName() + ".class";
    }

}
