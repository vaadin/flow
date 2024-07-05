/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
