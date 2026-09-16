/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.Serializable;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND;
import static com.vaadin.flow.internal.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.FrontendUtils.JS_INVOKERS_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGenerateJsInvokersTest {

    @JsInvoker
    public interface GreeterJs extends Serializable {
        @JsExpression("window.alert($0)")
        void showGreeting(String greeting);

        @JsExpression("window.alert('Hello')")
        void showGreeting();
    }

    @TempDir
    File temporaryFolder;

    private TaskGenerateJsInvokers task;
    private File frontendFolder;

    @BeforeEach
    void setUp() {
        frontendFolder = new File(temporaryFolder, FRONTEND);
        frontendFolder.mkdirs();
        Options options = new Options(Mockito.mock(Lookup.class),
                new DefaultClassFinder(Set.of(GreeterJs.class)), null)
                .withFrontendDirectory(frontendFolder);
        task = new TaskGenerateJsInvokers(options);
    }

    @Test
    void generatesAFunctionPerDeclaredExpression()
            throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertTrue(
                content.contains("window.Vaadin.Flow.jsInvokers[\""
                        + GreeterJs.class.getName() + "\"]"),
                "the invoker should be registered under its class name: "
                        + content);
        assertTrue(
                content.contains("\"showGreeting/1\": async function ($0) {"),
                "an overload should be keyed by name and argument count: "
                        + content);
        assertTrue(content.contains("window.alert($0)"),
                "the declared expression should be the body of the function: "
                        + content);
        assertTrue(content.contains("\"showGreeting/0\": async function () {"),
                "the no-argument overload should be generated too: " + content);
    }

    @Test
    void writesTheFileTheBootstrapImports() throws ExecutionFailedException {
        task.execute();

        assertTrue(
                new File(new File(frontendFolder, GENERATED),
                        JS_INVOKERS_FILE_NAME).exists(),
                "the generated file should be where the bootstrap imports it from");
    }
}
