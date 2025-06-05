/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
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

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.upload.testbench.UploadElement;
import com.vaadin.flow.spring.flowsecurity.views.PublicView;
import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
@Ignore("test PR.. no need to fail")
public class AppViewIT extends AbstractIT {

   
}
