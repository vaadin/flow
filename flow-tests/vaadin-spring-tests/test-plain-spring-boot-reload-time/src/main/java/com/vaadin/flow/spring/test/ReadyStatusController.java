/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReadyStatusController {

    private boolean ready = true;

    public ReadyStatusController() {
        System.out.println("ReadyStatusController created.");
    }

    public void setReady(boolean ready) {
        this.ready = ready;
        System.out.println("ReadyStatusController.setReady(" + ready + ")");
    }

    @GetMapping("/isready")
    public String isReady() {
        if (ready) {
            System.out.println("ReadyStatusController.isReady() returns "
                    + String.valueOf(ready));
        }
        return String.valueOf(ready);
    }
}
