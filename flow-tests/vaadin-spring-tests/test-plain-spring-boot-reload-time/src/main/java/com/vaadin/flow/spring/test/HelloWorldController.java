/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.context.ApplicationListener;
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
public class HelloWorldController
        implements ApplicationListener<ClassPathChangedEvent> {

    private boolean reloadTriggered = false;

    @GetMapping("/")
    public String page(Model model) {
        return "page";
    }

    /**
     * Triggers reload by touching Application Java file. As browser is not
     * reloading itself, this method waits and returns response only after
     * Spring boot sends a {@link ClassPathChangedEvent} on reload and notifies
     * listener in this controller. Browser reload is done immediately when
     * response is received.
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.OK)
    public void start() {
        reloadTriggered = false;
        Application.triggerReload();
        try {
            int waitedTimeMs = 0;
            int timeToWaitMs = 1;
            while (true) {
                waitedTimeMs += timeToWaitMs;
                Thread.sleep(timeToWaitMs);
                if (reloadTriggered) {
                    return;
                }
                if (waitedTimeMs > 5000) {
                    throw new RuntimeException(
                            "Gave up waiting reload event after five seconds.");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onApplicationEvent(ClassPathChangedEvent event) {
        System.out.println("Reload triggered");
        reloadTriggered = true;
    }
}
