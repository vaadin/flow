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
package com.vaadin.flow.spring.flowsecurity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.upload.testbench.UploadElement;
import com.vaadin.flow.spring.flowsecurity.views.PublicView;
import com.vaadin.testbench.TestBenchElement;

public class AppViewIT extends AbstractIT {

    private static final String LOGIN_PATH = "my/login/page";
    private static final String USER_FULLNAME = "John the User";
    private static final String ADMIN_FULLNAME = "Emma the Admin";

    private void logout() {
        if (!$(ButtonElement.class).attribute("id", "logout").exists()) {
            open("");
            assertRootPageShown();
        }
        clickLogout();
        assertRootPageShown();
    }

    private void clickLogout() {
        getMainView().$(ButtonElement.class).id("logout").click();
    }

    @Test
    public void root_page_does_not_require_login() {
        open("");
        assertRootPageShown();
    }

    @Test
    public void root_page_by_alias_does_not_require_login() {
        open("home");
        assertRootPageShown();
    }

    @Test
    public void other_public_page_does_not_require_login() {
        open("another");
        assertAnotherPublicPageShown();
    }

    @Test
    public void other_public_page_by_template_alias_does_not_require_login() {
        open("hey/guest/welcome/123");
        assertAnotherPublicPageShown();
        Assert.assertEquals("guest", $("span").id("p-name").getText());
        Assert.assertEquals("123", $("span").id("p-wild").getText());

        open("hey/anonymous/welcome/1/2/3");
        assertAnotherPublicPageShown();
        Assert.assertEquals("anonymous", $("span").id("p-name").getText());
        Assert.assertEquals("1/2/3", $("span").id("p-wild").getText());

        String path = "hey/anchor/welcome/home";
        $("a").attribute("href", path).first().click();
        assertPathShown(path);
        assertAnotherPublicPageShown();
        Assert.assertEquals("anchor", $("span").id("p-name").getText());
        Assert.assertEquals("home", $("span").id("p-wild").getText());

    }

    @Test
    public void navigate_to_private_view_prevented() {
        open("");
        navigateTo("private", false);
        assertLoginViewShown();
    }

    @Test
    public void navigate_to_admin_view_prevented() {
        open("");
        navigateTo("admin", false);
        assertLoginViewShown();
    }

    @Test
    public void redirect_to_private_view_after_login() {
        open("private");
        assertPathShown(LOGIN_PATH);
        loginUser();
        assertPrivatePageShown(USER_FULLNAME);
    }

    @Test
    @Ignore("Requires VaadinSavedRequestAwareAuthenticationSuccessHandler usually installed by VaadinWebSecurity#setLoginView ")
    public void redirect_to_private_view_after_navigation_and_login() {
        open("");
        navigateTo("private", false);
        assertPathShown(LOGIN_PATH);
        loginUser();
        assertPrivatePageShown(USER_FULLNAME);
    }

    @Test
    public void redirect_to_admin_view_after_login() {
        open("admin");
        assertPathShown(LOGIN_PATH);
        loginAdmin();
        assertAdminPageShown(ADMIN_FULLNAME);
    }

    @Test
    public void private_page_logout_should_redirect_to_root() {
        open(LOGIN_PATH);
        loginUser();
        navigateTo("private");
        clickLogout();
        assertRootPageShown();
    }

    @Test
    public void logout_redirects_to_root_page() {
        open(LOGIN_PATH);
        loginUser();
        navigateTo("private");
        assertPrivatePageShown(USER_FULLNAME);
        clickLogout();
        assertRootPageShown();
    }

    @Test
    public void redirect_to_resource_after_login() {
        String contents = "Secret document for admin";
        String path = "admin-only/secret.txt";
        openResource(path);
        loginAdmin();
        assertResourceShown(path);
        String result = getDriver().getPageSource();
        Assert.assertTrue(result.contains(contents));
    }

    @Test
    public void refresh_when_logged_in_stays_logged_in() {
        open("private");
        loginUser();
        assertPrivatePageShown(USER_FULLNAME);
        refresh();
        assertPrivatePageShown(USER_FULLNAME);
    }

    @Test
    public void access_restricted_to_logged_in_users() {
        String contents = "Secret document for all logged in users";
        String path = "all-logged-in/secret.txt";

        openResource(path);
        assertLoginViewShown();
        loginUser();
        assertPageContains(contents);
        logout();

        openResource(path);
        loginAdmin();
        assertPageContains(contents);
        logout();

        openResource(path);
        assertLoginViewShown();
    }

    @Test
    public void access_restricted_to_all_by_default() {
        String path = "restricted/secret.txt";

        openResource(path);
        assertLoginViewShown();
        loginUser();
        assertForbiddenPage();
        logout();

        openResource(path);
        loginAdmin();
        assertForbiddenPage();
        logout();

        openResource(path);
        assertLoginViewShown();
    }

    @Test
    public void access_restricted_to_admin() {
        String contents = "Secret document for admin";
        String path = "admin-only/secret.txt";
        openResource(path);
        assertLoginViewShown();
        loginUser();
        openResource(path);
        assertForbiddenPage();
        logout();

        openResource(path);
        loginAdmin();
        String adminResult = getDriver().getPageSource();
        // Assert.assertTrue(adminResult.contains(contents));
        MatcherAssert.assertThat(adminResult,
                CoreMatchers.containsString(contents));
        logout();
        openResource(path);
        assertLoginViewShown();
    }

    @Test
    public void public_app_resources_available_for_all() {
        openResource("public/public.txt");
        String shouldBeTextFile = getDriver().getPageSource();
        Assert.assertTrue(
                shouldBeTextFile.contains("Public document for all users"));
        open(LOGIN_PATH);
        loginUser();
        openResource("public/public.txt");
        shouldBeTextFile = getDriver().getPageSource();
        Assert.assertTrue(
                shouldBeTextFile.contains("Public document for all users"));
    }

    @Test
    public void upload_file_in_private_view() throws IOException {
        open("private");
        loginUser();
        UploadElement upload = $(UploadElement.class).first();
        File tmpFile = File.createTempFile("security-flow-image", ".png");
        InputStream imageStream = getClass().getClassLoader()
                .getResourceAsStream("image.png");
        IOUtils.copyLarge(imageStream, new FileOutputStream(tmpFile));
        tmpFile.deleteOnExit();
        upload.upload(tmpFile, 0);
        waitForUploads(upload, 60);

        TestBenchElement text = $("p").id("uploadText");
        TestBenchElement img = $("img").id("uploadImage");

        Assert.assertEquals("Loan application uploaded by John the User",
                text.getText());
        Assert.assertTrue(img.getPropertyString("src")
                .contains("/VAADIN/dynamic/resource/"));
    }

    @Test
    public void navigate_in_thread_without_access() {
        open("");
        $(ButtonElement.class).id(PublicView.BACKGROUND_NAVIGATION_ID).click();

        // This waits for longer than the delay in the UI so we do not need a
        // separate
        // sleep
        assertLoginViewShown();
    }

    @Test
    public void navigate_in_thread_with_access() {
        open(LOGIN_PATH);
        loginAdmin();
        $(ButtonElement.class).id(PublicView.BACKGROUND_NAVIGATION_ID).click();

        // This waits for longer than the delay in the UI so we do not need a
        // separate
        // sleep
        assertAdminPageShown(ADMIN_FULLNAME);
    }

    @Test
    public void anonymous_linkToPrivatePage_loginViewShown() {
        open("");
        $(TestBenchElement.class).id(PublicView.ANCHOR_NAVIGATION_ID).click();
        assertLoginViewShown();
    }

    @Test
    public void anonymous_linkToPrivatePageWithAlias_loginViewShown() {
        open("");
        $(TestBenchElement.class).id(PublicView.ANCHOR_ALIAS_NAVIGATION_ID)
                .click();
        assertLoginViewShown();
    }

    @Test
    public void anonymous_forwardToPrivatePage_loginViewShown() {
        open("");
        $(TestBenchElement.class).id(PublicView.FORWARD_NAVIGATION_ID).click();
        assertLoginViewShown();
    }

    @Test
    public void anonymous_rerouteToPrivatePage_loginViewShown() {
        open("");
        $(TestBenchElement.class).id(PublicView.REROUTE_NAVIGATION_ID).click();
        assertLoginViewShown();
    }

    @Test
    public void loggedInAsUser_linkToAdminPage_accessDenied() {
        open(LOGIN_PATH);
        loginUser();
        $(TestBenchElement.class).id(PublicView.ANCHOR_NAVIGATION_ID).click();
        assertNotFoundView("admin");
    }

    @Test
    public void loggedInAsUser_linkToAdminPageWithAlias_accessDenied() {
        open(LOGIN_PATH);
        loginUser();
        $(TestBenchElement.class).id(PublicView.ANCHOR_ALIAS_NAVIGATION_ID)
                .click();
        assertNotFoundView("alias-for-admin");
    }

    @Test
    public void loggedInAsUser_forwardToAdminPage_notFoundViewShown() {
        open(LOGIN_PATH);
        loginUser();
        $(TestBenchElement.class).id(PublicView.FORWARD_NAVIGATION_ID).click();
        assertNotFoundView("admin");
    }

    @Test
    public void loggedInAsUser_rerouteToAdminPage_notFoundViewShown() {
        open(LOGIN_PATH);
        loginUser();
        $(TestBenchElement.class).id(PublicView.REROUTE_NAVIGATION_ID).click();
        assertNotFoundView("admin");
    }

    private void navigateTo(String path) {
        navigateTo(path, true);
    }

    private void navigateTo(String path, boolean assertPathShown) {
        getMainView().$("a").attribute("href", path).first().click();
        if (assertPathShown) {
            assertPathShown(path);
        }
    }

    private TestBenchElement getMainView() {
        return waitUntil(driver -> $("*").id("main-view"));
    }

    private void refresh() {
        getDriver().navigate().refresh();
    }

    private void assertForbiddenPage() {
        assertPageContains(
                "There was an unexpected error (type=Forbidden, status=403).");
    }

    private void assertNotFoundView(String path) {
        assertPageContains("Could not navigate to '" + path + "'");
    }

    private void assertPageContains(String contents) {
        String pageSource = getDriver().getPageSource();
        Assert.assertTrue(pageSource.contains(contents));
    }

    private List<MenuItem> getMenuItems() {
        List<TestBenchElement> anchors = getMainView().$("vaadin-tabs").first()
                .$("a").all();

        return anchors.stream().map(anchor -> {
            String href = (String) anchor.callFunction("getAttribute", "href");
            String text = anchor.getPropertyString("textContent");
            boolean available = true;
            if (text.endsWith((" (hidden)"))) {
                text = text.replace(" (hidden)", "");
                available = false;
            }
            return new MenuItem(href, text, available);
        }).collect(Collectors.toList());
    }

    // Workaround for https://github.com/vaadin/flow-components/issues/3646
    // The issue causes the upload test to be flaky
    private void waitForUploads(UploadElement element, int maxSeconds) {
        WebDriver.Timeouts timeouts = getDriver().manage().timeouts();
        timeouts.scriptTimeout(Duration.ofSeconds(maxSeconds));

        String script = """
                var callback = arguments[arguments.length - 1];
                var upload = arguments[0];
                let intervalId;
                intervalId = window.setInterval(function() {
                  var inProgress = upload.files.filter(function(file) { return file.uploading;}).length >0;
                  if (!inProgress) {
                    window.clearInterval(intervalId);
                    callback();
                  }
                }, 500);
                """;
        getCommandExecutor().getDriver().executeAsyncScript(script, element);
    }

}
