package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.login.testbench.LoginFormElement;
import com.vaadin.flow.component.login.testbench.LoginOverlayElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AppViewIT extends ChromeBrowserTest {

    @After
    public void tearDown() {
        logout();
    }

    private void logout() {
        open("logout");
    }

    private void open(String path) {
        getDriver().get(getRootURL() + "/" + path);
    }

    @Test
    public void root_page_should_require_login() {
        // when the / route is opened
        open("");

        // then it redirects to the default login page
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/login"));

        // when the user logs in
        loginUser();

        // then it redirects to /secured and there are no client errors
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/"));
        Assert.assertNotNull(findElement(By.id("root")));
        checkLogsForErrors();
    }

    @Test
    public void deep_page_should_require_login() {
        // when the /secured route is opened
        open("secured");

        // then it redirects to the default login page
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/login"));

        // when the user logs in
        loginUser();

        // then it redirects to /secured and there are no client errors
        waitUntil(ExpectedConditions.urlToBe(getRootURL() + "/secured"));
        Assert.assertNotNull(findElement(By.id("secured")));
        checkLogsForErrors();
    }

    @Test
    public void access_restricted_to_logged_in_users() {
        String contents = "Secret document for all logged in users";
        String path = "all-logged-in/secret.txt";

        open(path);
        String anonResult = getDriver().getPageSource();
        Assert.assertFalse(anonResult.contains(contents));
        loginUser();
        open(path);
        String userResult = getDriver().getPageSource();
        Assert.assertTrue(userResult.contains(contents));
        logout();
        loginAdmin();
        open(path);
        String adminResult = getDriver().getPageSource();
        Assert.assertTrue(adminResult.contains(contents));
        logout();
        open(path);
        String anonResult2 = getDriver().getPageSource();
        Assert.assertFalse(anonResult2.contains(contents));

    }

    @Test
    public void access_restricted_to_admin() {
        String contents = "Secret document for admin";
        String path = "admin-only/secret.txt";
        open(path);
        String anonResult = getDriver().getPageSource();
        Assert.assertFalse(anonResult.contains(contents));
        loginUser();
        open(path);
        String userResult = getDriver().getPageSource();
        Assert.assertFalse(userResult.contains(contents));
        logout();
        loginAdmin();
        open(path);
        String adminResult = getDriver().getPageSource();
        Assert.assertTrue(adminResult.contains(contents));
        logout();
        open(path);
        String anonResult2 = getDriver().getPageSource();
        Assert.assertFalse(anonResult2.contains(contents));
    }

    @Test
    public void static_resources_accessible_without_login() throws Exception {
        open("manifest.webmanifest");
        Assert.assertTrue(getDriver().getPageSource().contains("\"name\":\"Spring Security Helper Test Project\""));
        open("sw.js");
        Assert.assertTrue(getDriver().getPageSource().contains("self.addEventListener(\"install\",this.install)"));
        open("sw-runtime-resources-precache.js");
        Assert.assertTrue(getDriver().getPageSource().contains("self.additionalManifestEntries = ["));
    }

    @Test
    public void public_app_resources_available_for_all() {
        open("public/public.txt");
        String shouldBeTextFile = getDriver().getPageSource();
        Assert.assertTrue(shouldBeTextFile.contains("Public document for all users"));
        open("login");
        loginUser();
        open("public/public.txt");
        shouldBeTextFile = getDriver().getPageSource();
        Assert.assertTrue(shouldBeTextFile.contains("Public document for all users"));
    }

    private void loginUser() {
        login("user", "user");
    }

    private void loginAdmin() {
        login("admin", "admin");
    }

    private void login(String username, String password) {
        LoginFormElement form = $(LoginOverlayElement.class).first().getLoginForm();
        form.getUsernameField().setValue(username);
        form.getPasswordField().setValue(password);
        form.submit();
        waitUntilNot(driver -> $(LoginOverlayElement.class).exists());
    }

}
