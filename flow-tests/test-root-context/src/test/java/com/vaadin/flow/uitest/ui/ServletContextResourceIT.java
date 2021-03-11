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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ServletContextResourceIT extends ChromeBrowserTest {

    @Test
    public void classResourceIsNotAvailable() throws IOException {
        URL url = new URL(getRootURL() + getPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assert.assertEquals(HttpURLConnection.HTTP_NOT_FOUND,
                connection.getResponseCode());
    }

    private String getPath() {
        Class<?> clazz = BaseHrefView.class;
        return "/view/" + clazz.getPackage().getName().replace('.', '/') + "/"
                + clazz.getSimpleName() + ".class";
    }

}
