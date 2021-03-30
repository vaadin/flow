package com.vaadin.flow.spring.test;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AppViewIT extends ChromeBrowserTest {

    @After
    public void tearDown() {
        getDriver().get(getRootURL() + "/logout");
    }

    @Test
    public void root_page_should_require_login() {
        // when the / route is opened
        getDriver().get(getRootURL() + "/");

        // then it redirects to the default login page
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/login"));

        // when the user logs in
        login();

        // then it redirects to /secured and there are no client errors
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/"));
        Assert.assertNotNull(findElement(By.id("root")));
        checkLogsForErrors();
    }

    @Test
    public void deep_page_should_require_login() {
        // when the /secured route is opened
        getDriver().get(getRootURL() + "/secured");

        // then it redirects to the default login page
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/login"));

        // when the user logs in
        login();

        // then it redirects to /secured and there are no client errors
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/secured"));
        Assert.assertNotNull(findElement(By.id("secured")));
        checkLogsForErrors();
    }

    @Test
    public void static_resources_accessible_without_login() throws Exception {
        verifyResponseCode("/manifest.webmanifest", 200);
        verifyResponseCode("/sw.js", 200);
        verifyResponseCode("/sw-runtime-resources-precache.js", 200);
        verifyResponseCode("/offline.html", 200);
    }

    @Test
    public void other_static_resources_secured() throws Exception {
        // expect redirect
        verifyResponseCode("/secured.html", 302);
        // Images and Icons are application specific and not related to Vaadin
        // See https://github.com/vaadin/flow/pull/10428
        verifyResponseCode("/images/image.png", 302);
        verifyResponseCode("/icons/icon.png", 302);
    }

    private void login() {
        findElement(By.id("username")).sendKeys("user");
        findElement(By.id("password")).sendKeys("user");
        findElement(By.tagName("button")).click();
    }

    private void verifyResponseCode(String path, int expectedCode) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableRedirectHandling().build();
        HttpGet httpGet = new HttpGet(getRootURL() + path);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            Assert.assertEquals(expectedCode, response.getStatusLine().getStatusCode());
        } finally {
            response.close();
        }
    }
}
