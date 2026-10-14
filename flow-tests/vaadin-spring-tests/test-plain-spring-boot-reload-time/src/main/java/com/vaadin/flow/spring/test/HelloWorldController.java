/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller for page.html to measure reload time of Hello world Spring boot
 * app. page.html uses benchmark.js to do the actual measuring. Page is mapped
 * to '/' path. POST to '/start' will trigger reload by touching Application
 * Java file.
 */
@Controller
public class HelloWorldController {

    private final ReadyStatusController readyStatusController;

    public HelloWorldController(ReadyStatusController readyStatusController) {
        this.readyStatusController = readyStatusController;
    }

    @GetMapping("/")
    public String page(Model model) {
        return "page";
    }

    /**
     * Triggers reload by touching Application Java file. As browser is not
     * reloading itself, this method marks {@link ReadyStatusController} ready
     * state to false. Client starts polling '/isready' until bean is reloaded
     * and returns true and then client reloads itself.
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.OK)
    public void start() {
        readyStatusController.setReady(false);
        Application.triggerReload();
    }

}
