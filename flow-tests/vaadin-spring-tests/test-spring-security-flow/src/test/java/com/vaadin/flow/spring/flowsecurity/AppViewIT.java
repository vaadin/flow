package com.vaadin.flow.spring.flowsecurity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.vaadin.flow.component.applayout.testbench.AppLayoutElement;
import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.upload.testbench.UploadElement;
import com.vaadin.flow.spring.flowsecurity.views.AdminView;
import com.vaadin.flow.spring.flowsecurity.views.PublicView;
import com.vaadin.testbench.TestBenchElement;

public class AppViewIT extends AbstractIT {

    protected static final String LOGIN_PATH = "my/login/page";
    protected static final String USER_FULLNAME = "John the User";
    protected static final String ADMIN_FULLNAME = "Emma the Admin";

    private void logout() {
        if (!$(ButtonElement.class).withAttribute("id", "logout").exists()) {
            open("");
            assertRootPageShown();
        }
        clickLogout();
        assertRootPageShown();
    }

    private void clickLogout() {
        getMainView().$(ButtonElement.class).id("logout").click();
    }

    private void clickLogoutAnchor() {
        getMainView().$("a").id("logout-anchor").click();
    }

    private void clickAccessRolePrefixedAdminPageFromThread() {
        getMainView().$(ButtonElement.class)
                .id(AdminView.ROLE_PREFIX_TEST_BUTTON_ID).click();
    }

    @Test
    public void menu_correct_for_anonymous() {
        open("");
        List<MenuItem> menuItems = getMenuItems();
        List<MenuItem> expectedItems = new ArrayList<MenuItem>();
        expectedItems.add(new MenuItem("", "Public", true));
        expectedItems.add(new MenuItem("private", "Private", false));
        expectedItems.add(new MenuItem("admin", "Admin", false));
        Assert.assertEquals(expectedItems, menuItems);
    }

    @Test
    public void menu_correct_for_user() {
        open(LOGIN_PATH);
        loginUser();
        List<MenuItem> menuItems = getMenuItems();
        List<MenuItem> expectedItems = new ArrayList<MenuItem>();
        expectedItems.add(new MenuItem("", "Public", true));
        expectedItems.add(new MenuItem("private", "Private", true));
        expectedItems.add(new MenuItem("admin", "Admin", false));
        Assert.assertEquals(expectedItems, menuItems);
    }

    @Test
    public void menu_correct_for_admin() {
        open(LOGIN_PATH);
        loginAdmin();
        List<MenuItem> menuItems = getMenuItems();
        List<MenuItem> expectedItems = new ArrayList<MenuItem>();
        expectedItems.add(new MenuItem("", "Public", true));
        expectedItems.add(new MenuItem("private", "Private", true));
        expectedItems.add(new MenuItem("admin", "Admin", true));
        Assert.assertEquals(expectedItems, menuItems);
    }

    @Test
    public void root_page_does_not_require_login() {
        open("");
        assertRootPageShown();
    }

    @Test
    public void other_public_page_does_not_require_login() {
        open("another");
        assertAnotherPublicPageShown();
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
    public void redirect_to_role_prefixed_admin_view_after_login() {
        open("admin");
        assertPathShown(LOGIN_PATH);
        loginAdmin();
        assertAdminPageShown(ADMIN_FULLNAME);
        clickAccessRolePrefixedAdminPageFromThread();
        assertRolePrefixedAdminPageShown(ADMIN_FULLNAME);
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
        Assert.assertTrue(adminResult.contains(contents));
        logout();
        openResource(path);
        assertLoginViewShown();
    }

    @Test
    public void static_resources_accessible_without_login() throws Exception {
        open("manifest.webmanifest");
        Assert.assertTrue(getDriver().getPageSource()
                .contains("\"name\":\"Spring Security Helper Test Project\""));
        open("sw.js");
        Assert.assertTrue(getDriver().getPageSource()
                .contains("this._installAndActiveListenersAdded"));
        open("sw-runtime-resources-precache.js");
        Assert.assertTrue(getDriver().getPageSource()
                .contains("self.additionalManifestEntries = ["));
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
        waitForClientRouter();
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

    // https://github.com/vaadin/flow/issues/7323
    @Test
    public void logout_via_doLogoutURL_redirects_to_logout() {
        open(LOGIN_PATH);
        loginAdmin();
        navigateTo("admin");
        assertAdminPageShown(ADMIN_FULLNAME);
        clickLogoutAnchor();
        assertLogoutViewShown();
    }

    @Test
    public void logout_server_initiated_redirects_to_logout() {
        open(LOGIN_PATH);
        loginAdmin();
        navigateTo("admin");
        assertAdminPageShown(ADMIN_FULLNAME);
        getMainView().$(ButtonElement.class).id("logout-server").click();
        assertRootPageShown();
    }

    @Test
    public void client_menu_routes_correct_for_anonymous() {
        navigateToClientMenuList();
        assertMenuListContains("PublicView");
    }

    @Test
    public void client_menu_routes_correct_for_user() {
        open(LOGIN_PATH);
        loginUser();
        navigateToClientMenuList();
        assertMenuListContains("PublicView, PrivateView");
    }

    @Test
    public void client_menu_routes_correct_for_admin() {
        open(LOGIN_PATH);
        loginAdmin();
        navigateToClientMenuList();
        assertMenuListContains("PublicView, PrivateView, AdminView");
    }

    @Test
    public void admin_impersonate_user_shows_expected() {
        Assume.assumeTrue(getUrlMappingBasePath().equals(""));

        open(LOGIN_PATH);
        loginAdmin();

        List<MenuItem> menuItems = getMenuItems();
        List<MenuItem> expectedItems = new ArrayList<>();

        expectedItems.add(new MenuItem("", "Public", true));
        expectedItems.add(new MenuItem("private", "Private", true));
        expectedItems.add(new MenuItem("admin", "Admin", true));
        Assert.assertEquals(expectedItems, menuItems);

        $(AppLayoutElement.class).first().setDrawerOpened(true);

        Assert.assertTrue(
                $(ButtonElement.class).id("impersonate").isDisplayed());

        $(ButtonElement.class).id("impersonate").click();

        expectedItems.clear();
        menuItems = getMenuItems();
        expectedItems.add(new MenuItem("", "Public", true));
        expectedItems.add(new MenuItem("private", "Private", true));
        expectedItems.add(new MenuItem("admin", "Admin", false));
        Assert.assertEquals(expectedItems, menuItems);

        $(AppLayoutElement.class).first().setDrawerOpened(true);

        Assert.assertTrue(
                $(ButtonElement.class).id("exit-impersonate").isDisplayed());
        $(ButtonElement.class).id("exit-impersonate").click();

        expectedItems.clear();
        menuItems = getMenuItems();
        expectedItems.add(new MenuItem("", "Public", true));
        expectedItems.add(new MenuItem("private", "Private", true));
        expectedItems.add(new MenuItem("admin", "Admin", true));
        Assert.assertEquals(expectedItems, menuItems);
    }

    private void assertMenuListContains(String expected) {
        TestBenchElement menuList = waitUntil(driver -> $("*").id("menu-list"));
        String menuListText = menuList.getText();
        Assert.assertTrue(menuListText.contains(expected));
    }

    private void navigateToClientMenuList() {
        open("menu-list");
        assertPathShown("menu-list");
    }

    protected void navigateTo(String path) {
        navigateTo(path, true);
    }

    protected void navigateTo(String path, boolean assertPathShown) {
        getMainView().$("a").attribute("href", path).first().click();
        if (assertPathShown) {
            assertPathShown(path);
        }
    }

    private TestBenchElement getMainView() {
        waitForClientRouter();
        return waitUntil(driver -> $("*").id("main-view"));
    }

    private void refresh() {
        getDriver().navigate().refresh();
    }

    private void assertForbiddenPage() {
        assertPageContains(
                "There was an unexpected error (type=Forbidden, status=403).");
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

    /*
     * The same driver is used to access both Vaadin views and static resources.
     * Static caching done by #isClientRouter can cause some tests to be flaky.
     */
    protected void waitForClientRouter() {
        AtomicBoolean hasClientRouter = new AtomicBoolean(false);
        // Tries the JS execution several times, to prevent failures caused
        // by redirects and page reloads, such as the following error seen
        // more frequently with Chrome 132
        // aborted by navigation: loader has changed while resolving nodes
        waitUntil(d -> {
            try {
                hasClientRouter.set((boolean) executeScript(
                        "return !!window.Vaadin.Flow.clients.TypeScript"));
                return true;
            } catch (WebDriverException expected) {
                return false;
            }
        });
        if (hasClientRouter.get()) {
            waitForElementPresent(By.cssSelector("#outlet > *"));
        }
    }

}
