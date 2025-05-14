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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@SpringBootApplication
@Configuration
@EnableWebSecurity
@Import(DummyOAuth2Server.class)
public class TestServletInitializer {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication
                .run(TestServletInitializer.class, args);
        ShutdownHook.registerShutdownHook(applicationContext::close);
    }

    /**
     * Requests to /shutdown will trigger application shutdown. This is needed
     * e.g. when running executable jar with exec-maven-plugin to stop the
     * server after ITs are done.
     */
    @Controller
    public static class ShutdownController {

        @GetMapping("/shutdown")
        @ResponseStatus(HttpStatus.OK)
        public void shutdown() {
            System.out.println("Shutdown request received.");
            CompletableFuture.runAsync(() -> ShutdownHook.shutdownHook.run(),
                    CompletableFuture.delayedExecutor(50,
                            TimeUnit.MILLISECONDS));
        }
    }
}
