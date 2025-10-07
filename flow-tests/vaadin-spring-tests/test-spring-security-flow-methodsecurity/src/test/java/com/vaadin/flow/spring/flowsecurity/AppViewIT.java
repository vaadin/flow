package com.vaadin.flow.spring.flowsecurity;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;

public class AppViewIT extends AbstractIT {

    private static final String LOGIN_PATH = "my/login/page";
    private static final String USER_FULLNAME = "John the User";
    private static final String ADMIN_FULLNAME = "Emma the Admin";

    @Test
    public void userAppliesForLoan_accessGranted() {
        open("private");
        assertPathShown(LOGIN_PATH);
        loginUser();
        assertPrivatePageShown(USER_FULLNAME);

        $(ButtonElement.class).id("applyForLoan").click();
        String balance = $("span").id("balanceText").getText();
        Assert.assertEquals(
                "Hello John the User, your bank account balance is $20000.00.",
                balance);
    }

    @Test
    public void userAppliesForHugeLoan_accessDenied() {
        open("private");
        assertPathShown(LOGIN_PATH);
        loginUser();
        assertPrivatePageShown(USER_FULLNAME);

        $(ButtonElement.class).id("applyForHugeLoan").click();
        String notification = $(NotificationElement.class).waitForFirst()
                .getText();
        Assert.assertEquals("Application failed: Access Denied", notification);
    }

    @Test
    public void adminAppliesForHugeLoan_accessGranted() {
        open("private");
        assertPathShown(LOGIN_PATH);
        loginAdmin();
        assertPrivatePageShown(ADMIN_FULLNAME);

        $(ButtonElement.class).id("applyForHugeLoan").click();
        String balance = $("span").id("balanceText").getText();
        Assert.assertEquals(
                "Hello Emma the Admin, your bank account balance is $1200000.00.",
                balance);
    }

    @Test
    public void adminAppliesForLoan_accessDenied() {
        open("private");
        assertPathShown(LOGIN_PATH);
        loginAdmin();
        assertPrivatePageShown(ADMIN_FULLNAME);

        $(ButtonElement.class).id("applyForLoan").click();
        String notification = $(NotificationElement.class).waitForFirst()
                .getText();
        Assert.assertEquals("Application failed: Access Denied", notification);
    }

}
