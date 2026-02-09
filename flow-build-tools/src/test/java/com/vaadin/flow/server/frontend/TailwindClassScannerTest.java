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
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TailwindClassScannerTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File sourceDir;

    @Before
    public void setup() throws IOException {
        sourceDir = tempDir.newFolder("src", "main", "java");
    }

    @Test
    public void extractClassNames_simpleAddClassName() throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        addClassName("bg-blue-500");
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(1, classNames.size());
        assertTrue(classNames.contains("bg-blue-500"));
    }

    @Test
    public void extractClassNames_multipleClassNames() throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        addClassNames("flex", "flex-col", "items-center");
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("flex"));
        assertTrue(classNames.contains("flex-col"));
        assertTrue(classNames.contains("items-center"));
    }

    @Test
    public void extractClassNames_spaceSeparatedInSingleString()
            throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        addClassName("p-4 m-2 bg-white rounded-lg");
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(4, classNames.size());
        assertTrue(classNames.contains("p-4"));
        assertTrue(classNames.contains("m-2"));
        assertTrue(classNames.contains("bg-white"));
        assertTrue(classNames.contains("rounded-lg"));
    }

    @Test
    public void extractClassNames_multilineMethodCall() throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        addClassNames("font-sans", "box-border",
                                "w-full", "h-full", "flex",
                                "flex-col", "items-center");
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(7, classNames.size());
        assertTrue(classNames.contains("font-sans"));
        assertTrue(classNames.contains("flex-col"));
    }

    @Test
    public void extractClassNames_setClassNameMethod() throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        setClassName("text-center");
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(1, classNames.size());
        assertTrue(classNames.contains("text-center"));
    }

    @Test
    public void extractClassNames_multipleMethods() throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        addClassName("bg-gray-100");
                        var div = new Div();
                        div.addClassName("text-red-500");
                        div.setClassName("p-4");
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(3, classNames.size());
        assertTrue(classNames.contains("bg-gray-100"));
        assertTrue(classNames.contains("text-red-500"));
        assertTrue(classNames.contains("p-4"));
    }

    @Test
    public void extractClassNames_singleQuotes() throws IOException {
        File javaFile = createJavaFile("TestView.java", """
                package com.example;
                import com.vaadin.flow.component.html.Div;

                public class TestView extends Div {
                    public TestView() {
                        addClassName('bg-blue-500');
                    }
                }
                """);

        List<String> classNames = TailwindClassScanner
                .extractClassNamesFromFile(javaFile);

        assertEquals(1, classNames.size());
        assertTrue(classNames.contains("bg-blue-500"));
    }

    @Test
    public void scanAndHashClassNames_generatesConsistentHash()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                    }
                }
                """);

        String hash1 = TailwindClassScanner.scanAndHashClassNames(sourceDir);
        String hash2 = TailwindClassScanner.scanAndHashClassNames(sourceDir);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    public void scanAndHashClassNames_differentClassesDifferentHash()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                    }
                }
                """);

        String hash1 = TailwindClassScanner.scanAndHashClassNames(sourceDir);

        // Modify the class
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-red-500");
                    }
                }
                """);

        String hash2 = TailwindClassScanner.scanAndHashClassNames(sourceDir);

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void haveClassNamesChanged_firstScan_returnsTrue()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                    }
                }
                """);

        boolean changed = TailwindClassScanner
                .haveClassNamesChanged(sourceDir, null);

        assertTrue(changed);
    }

    @Test
    public void haveClassNamesChanged_noChange_returnsFalse()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                    }
                }
                """);

        String initialHash = TailwindClassScanner
                .scanAndHashClassNames(sourceDir);

        boolean changed = TailwindClassScanner
                .haveClassNamesChanged(sourceDir, initialHash);

        assertFalse(changed);
    }

    @Test
    public void haveClassNamesChanged_classAdded_returnsTrue()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                    }
                }
                """);

        String initialHash = TailwindClassScanner
                .scanAndHashClassNames(sourceDir);

        // Add a new class
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                        addClassName("text-white");
                    }
                }
                """);

        boolean changed = TailwindClassScanner
                .haveClassNamesChanged(sourceDir, initialHash);

        assertTrue(changed);
    }

    @Test
    public void haveClassNamesChanged_classChanged_returnsTrue()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-lime-100");
                    }
                }
                """);

        String initialHash = TailwindClassScanner
                .scanAndHashClassNames(sourceDir);

        // Change the class (the issue #22724 scenario)
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-lime-200");
                    }
                }
                """);

        boolean changed = TailwindClassScanner
                .haveClassNamesChanged(sourceDir, initialHash);

        assertTrue(changed);
    }

    @Test
    public void haveClassNamesChanged_codeChangedButNotClasses_returnsFalse()
            throws IOException {
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                        System.out.println("Hello");
                    }
                }
                """);

        String initialHash = TailwindClassScanner
                .scanAndHashClassNames(sourceDir);

        // Change non-class-related code
        createJavaFile("View1.java", """
                package com.example;
                public class View1 {
                    void init() {
                        addClassName("bg-blue-500");
                        System.out.println("World");
                    }
                }
                """);

        boolean changed = TailwindClassScanner
                .haveClassNamesChanged(sourceDir, initialHash);

        assertFalse(changed);
    }

    private File createJavaFile(String fileName, String content)
            throws IOException {
        File file = new File(sourceDir, fileName);
        Files.writeString(file.toPath(), content);
        return file;
    }
}
