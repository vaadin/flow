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
