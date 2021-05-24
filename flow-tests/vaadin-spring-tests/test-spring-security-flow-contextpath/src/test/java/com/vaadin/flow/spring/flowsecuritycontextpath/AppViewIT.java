package com.vaadin.flow.spring.flowsecuritycontextpath;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.login.testbench.LoginFormElement;
import com.vaadin.flow.component.login.testbench.LoginOverlayElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context";
    }
}
