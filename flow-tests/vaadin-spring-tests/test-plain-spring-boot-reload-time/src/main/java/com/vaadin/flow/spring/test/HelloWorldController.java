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
