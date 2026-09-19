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
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvoker;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND;
import static com.vaadin.flow.internal.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.FrontendUtils.JS_INVOKERS_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TaskGenerateJsInvokersTest {

    @JsInvoker
    public interface GreeterJs extends Serializable {
        @JsExpression("window.alert({ text: $0, kind: 'greeting' })")
        void showGreeting(String greeting);

        @JsExpression("window.alert('Hello')")
        void showGreeting();
    }

    @JsInvoker
    public interface CounterJs extends Serializable {
        @JsExpression("this.count = ($0 || 0) + 1")
        void count(Integer from);
    }

    @JsInvoker
    public interface NothingJs extends Serializable {
        void notDeclared();
    }

    @TempDir
    File temporaryFolder;

    private TaskGenerateJsInvokers task;
    private Options options;
    private File frontendFolder;

    @BeforeEach
    void setUp() {
        frontendFolder = new File(temporaryFolder, FRONTEND);
        frontendFolder.mkdirs();
        options = new Options(Mockito.mock(Lookup.class),
                new DefaultClassFinder(
                        Set.of(GreeterJs.class, NothingJs.class)),
                null).withFrontendDirectory(frontendFolder);
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
        assertTrue(
                content.contains(
                        "window.alert({ text: $0, kind: 'greeting' })"),
                "the declared expression should be the body of the function, as it was written: "
                        + content);
        assertTrue(content.contains("\"showGreeting/0\": async function () {"),
                "the no-argument overload should be generated too: " + content);
    }

    @Test
    void invokerWithoutDeclaredJavaScript_isNotRegistered()
            throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertFalse(content.contains(NothingJs.class.getName()),
                "an interface that declares no JavaScript has nothing to register: "
                        + content);
    }

    @Test
    void updateJsInvokers_dropsAnInvokerTheFileNamesAndNothingHas()
            throws ExecutionFailedException, IOException {
        task.execute();
        // What a file written by an older state of the application looks like:
        // it names an interface that is no longer there to render
        File generated = new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_INVOKERS_FILE_NAME);
        Files.writeString(generated.toPath(),
                Files.readString(generated.toPath()).replace(
                        NothingJs.class.getName(), "com.example.GoneJs"));

        List<Class<?>> missing = TaskGenerateJsInvokers
                .updateJsInvokers(options, List.of(GreeterJs.class));

        assertTrue(missing.isEmpty(),
                "the interface that was asked for should be in the file");
        assertFalse(
                Files.readString(generated.toPath())
                        .contains("com.example.GoneJs"),
                "a name the file holds that nothing answers to should be dropped");
    }

    @Test
    void updateJsInvokers_fileNotWritable_answersWithWhatItDoesNotCarry()
            throws ExecutionFailedException {
        // A file that carries one of the two interfaces, and a folder nothing
        // can be written into
        task.execute();
        File generatedFolder = FrontendUtils
                .getFrontendGeneratedFolder(frontendFolder);
        assumeTrue(generatedFolder.setWritable(false),
                "the folder has to be made read only for this");

        try {
            List<Class<?>> missing = TaskGenerateJsInvokers.updateJsInvokers(
                    options, List.of(GreeterJs.class, CounterJs.class));

            assertEquals(List.of(CounterJs.class), missing,
                    "the interface the file carries is not missing because the write failed");
        } finally {
            generatedFolder.setWritable(true);
        }
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
