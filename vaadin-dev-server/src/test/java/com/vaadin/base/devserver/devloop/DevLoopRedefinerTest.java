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
package com.vaadin.base.devserver.devloop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The connector must always answer exactly one line, even on failure, or the
 * daemon blocks waiting for a reply that is not coming.
 * <p>
 * Of the reply itself, what is unit-testable is what needs no running
 * application: the no-agent and no-hotswapper paths, the decision
 * {@code inspect} reaches about a change-set, and the questions asked of a
 * class or of its compiled bytes. Defining a class and refreshing Flow need a
 * real {@code Instrumentation} handle and a live service, and are covered end
 * to end in {@code flow-tests/test-devloop}.
 */
class DevLoopRedefinerTest {

    @Test
    void redefine_withoutAnAgent_reportsTheReasonInTheProtocolVocabulary() {
        String reply = DevLoopRedefiner.redefine("com.example.Foo");

        // The daemon parses "status" from the first token and escalates to a
        // restart on anything but OK, so the kind has to be machine-readable.
        assertTrue(reply.startsWith("ERR kind="), reply);
        assertTrue(reply.contains("kind=no-agent")
                || reply.contains("kind=no-hotswapper"), reply);
    }

    @Test
    void resources_withoutAHotswapper_reportsIt() {
        String reply = DevLoopRedefiner.resources("/tmp/a.css");

        assertTrue(reply.startsWith("ERR kind=no-hotswapper"), reply);
    }

    @Test
    void info_answersEvenWithNothingRegistered() {
        String reply = DevLoopRedefiner.info();

        // status() is what a daemon reads first; an unregistered app still has
        // to say so in one line rather than throw.
        assertTrue(reply.startsWith("OK "), reply);
        assertTrue(reply.contains("hotswapper=false"), reply);
        assertTrue(reply.contains("frontend=unknown"), reply);
    }

    @Test
    void frontendStatus_withoutAService_isUnknown() {
        assertTrue("unknown".equals(DevLoopRedefiner.frontendStatus()));
    }

    @Test
    void theme_withoutAService_reportsIt() {
        String reply = DevLoopRedefiner.theme("/tmp/themes/t/styles.css");

        assertTrue(reply.startsWith("ERR kind=no-service"), reply);
    }

    @Test
    void reload_withoutABrowser_stillAnswersInOneLine() {
        String reply = DevLoopRedefiner.reload();

        // No live reload to ask means no browser was reloaded, which is a fact
        // the daemon reports rather than an error it fails on.
        assertTrue(reply.equals("OK reloaded=false"), reply);
    }

    @Test
    void publicRootOf_takesTheLastWholeSegment() {
        // The marker directory is part of the root: the URL the browser knows
        // is the path below it, so a root short by one segment pushes CSS under
        // a URL that matches nothing on the page.
        assertEquals(file("/p/src/main/resources/META-INF/resources"),
                DevLoopRedefiner.publicRootOf(
                        "/p/src/main/resources/META-INF/resources/app.css"));

        // "/resources/" also occurs in "src/main/resources", earlier. Taking
        // the first match would make the URL "META-INF/resources/app.css"
        // above, and "resources/a.css" here.
        assertEquals(file("/p/src/main/resources/resources"), DevLoopRedefiner
                .publicRootOf("/p/src/main/resources/resources/a.css"));

        // A directory named like a marker anywhere above the real root must not
        // win, however deep the checkout sits.
        assertEquals(file("/static-site/p/src/main/resources/static"),
                DevLoopRedefiner.publicRootOf(
                        "/static-site/p/src/main/resources/static/a.css"));

        assertEquals(file("C:/p/src/main/webapp"), DevLoopRedefiner
                .publicRootOf("C:\\p\\src\\main\\webapp\\css\\a.css"));

        // Under no public root at all, so there is nothing to resolve against
        // and the caller falls back to a reload.
        assertNull(DevLoopRedefiner
                .publicRootOf("/p/src/main/java/com/example/View.java"));
    }

    private static File file(String path) {
        return new File(path);
    }

    /** Where {@code @Theme} has to sit, and it is never a Component. */
    @Theme(value = "my-theme", variant = "dark")
    static class ThemedAppShell implements AppShellConfigurator {
    }

    /** {@code @JsModule} and friends are not Component-only either. */
    @JsModule("./holder.js")
    @NpmPackage(value = "some-pkg", version = "1.2.3")
    static class PlainFrontendHolder {
    }

    @Tag("some-view")
    @JsModule("./view.js")
    static class SomeView extends Component {
    }

    static class NothingDeclared {
    }

    @Test
    void frontendDependencies_seesTheThemeOnAnAppShellThatIsNoComponent() {
        // @Theme belongs on the AppShellConfigurator, which is never a
        // Component. Read only from Components, it is invisible - so adding or
        // changing a theme redefines cleanly, reports frontendImports=-, and
        // apply calls Stable over a theme the build never generated imports
        // for.
        String imports = DevLoopRedefiner
                .frontendDependencies(ThemedAppShell.class);

        assertTrue(imports.contains("theme:my-theme"), imports);
        // The variant is read at startup the same way the name is, so a change
        // to it is the same kind of change.
        assertTrue(imports.contains("dark"), imports);
    }

    @Test
    void frontendDependencies_seesBuildTimeImportsOnANonComponent() {
        String imports = DevLoopRedefiner
                .frontendDependencies(PlainFrontendHolder.class);

        assertTrue(imports.contains("js:./holder.js"), imports);
        assertTrue(imports.contains("npm:some-pkg@1.2.3"), imports);
    }

    @Test
    void frontendDependencies_readsAComponentAndAnswersEmptyForNeither() {
        // The Component path goes through AnnotationReader, so an import
        // inherited from a supertype or picked up through @Uses still counts
        // the way the build counts it.
        assertTrue(DevLoopRedefiner.frontendDependencies(SomeView.class)
                .contains("js:./view.js"));
        // And a class that declares none is not a change to any.
        assertEquals("",
                DevLoopRedefiner.frontendDependencies(NothingDeclared.class));
    }

    @Test
    void declaresFromBytes_answersForAClassNothingHasLoaded()
            throws IOException {
        // The only questions that can be asked about a class the JVM has never
        // loaded, and both have to be, because a context that scanned before
        // the class existed has no bean definition for it and a metamodel
        // built then maps no entity that appeared afterwards.
        byte[] plain = classBytes(NothingDeclared.class);

        assertFalse(DevLoopRedefiner.declaresSpringBean(plain), "plain class");
        assertFalse(DevLoopRedefiner.declaresEntity(plain), "plain class");
        // Every entry, spelled out rather than read from the production list -
        // which would pass whatever that list happened to say. A stereotype is
        // matched by exactly one literal, so a typo in one of them is one
        // annotation that silently stops escalating while the rest keep
        // working, and this is where that is cheap to rule out. Spelled out
        // rather than compiled in for a second reason too: Spring is not on
        // this module's classpath, and what the check reads is the descriptor
        // javac writes into the constant pool.
        //
        // @RestController and the two advice annotations are listed in their
        // own right because each is a @Component through a meta-annotation
        // that the annotated class's own constant pool never mentions.
        for (String stereotype : List.of(
                "Lorg/springframework/stereotype/Component;",
                "Lorg/springframework/stereotype/Service;",
                "Lorg/springframework/stereotype/Repository;",
                "Lorg/springframework/stereotype/Controller;",
                "Lorg/springframework/web/bind/annotation/RestController;",
                "Lorg/springframework/web/bind/annotation/ControllerAdvice;",
                "Lorg/springframework/web/bind/annotation/RestControllerAdvice;",
                "Lorg/springframework/context/annotation/Configuration;")) {
            assertTrue(DevLoopRedefiner.declaresSpringBean(
                    withConstant(plain, stereotype)), stereotype);
        }
        // The two escalate on separate fields and carry separate reasons, so
        // neither may answer for the other.
        assertFalse(DevLoopRedefiner.declaresSpringBean(
                withConstant(plain, "Ljakarta/persistence/Entity;")));
        assertTrue(DevLoopRedefiner.declaresEntity(
                withConstant(plain, "Ljakarta/persistence/Entity;")));
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        try (InputStream in = DevLoopRedefinerTest.class.getResourceAsStream(
                "/" + type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }

    /**
     * A real class file with one more entry in its constant pool, which is what
     * the check reads and all it reads.
     */
    private static byte[] withConstant(byte[] bytes, String descriptor) {
        byte[] added = descriptor.getBytes(StandardCharsets.ISO_8859_1);
        byte[] joined = Arrays.copyOf(bytes, bytes.length + added.length);
        System.arraycopy(added, 0, joined, bytes.length, added.length);
        return joined;
    }

    @Test
    void inspect_readsALoadedClassAndTheBytesItIsAboutToBeGiven() {
        String plain = NothingDeclared.class.getName();
        String view = SomeView.class.getName();

        // The same class twice is what a duplicate loaded copy looks like, and
        // both have to go into the one redefine call: redefining one leaves
        // the copy the application instantiates untouched, which is a green
        // apply over a stale page.
        DevLoopRedefiner.Inspection inspected = DevLoopRedefiner.inspect(
                List.of(plain, view),
                Map.of(plain,
                        List.of(NothingDeclared.class, NothingDeclared.class),
                        view, List.of(SomeView.class)),
                List.of(testClasses()));

        assertNull(inspected.error());
        assertEquals(3, inspected.definitions().size());
        assertEquals(1, inspected.duplicates());
        assertTrue(inspected.notLoaded().isEmpty());
        // What the loaded class says, which is the other source and reaches
        // the daemon on its own field: onHotswap visibly refreshes a
        // Component, and a change-set with none is reported as live but not
        // yet visible rather than simply stable. Under the binary tail of the
        // name, which is what a nested type is reported as.
        assertEquals(Set.of("DevLoopRedefinerTest$SomeView"),
                inspected.uiClasses());
        // Neither of them is a bean or an entity, by either source: the
        // redefine is the whole of this change.
        assertTrue(inspected.entities().isEmpty(), "entities");
        assertTrue(inspected.stereotypes().isEmpty(), "stereotypes");
        assertTrue(inspected.beans().isEmpty(), "beans");
    }

    @Test
    void inspect_namesTheBeansAndEntitiesThisJvmHasNoClassFor(
            @TempDir Path classes) throws IOException {
        // A class that did not exist when the application started. There is
        // nothing loaded to redefine, so every signal read off a loaded class
        // is empty and only the new bytes say anything - and what they say is
        // that component scanning and the metamodel, both of which ran at
        // startup, know nothing about these.
        writeClass(classes, "bean.NewBean",
                "Lorg/springframework/stereotype/Service;");
        writeClass(classes, "model.NewEntity", "Ljakarta/persistence/Entity;");

        DevLoopRedefiner.Inspection inspected = DevLoopRedefiner.inspect(
                List.of("bean.NewBean", "model.NewEntity", "gone.Nothing"),
                Map.of(), List.of(classes));

        // The third one is neither loaded nor on the search path - a class
        // from outside the loop - and is no error and nothing to answer for.
        assertNull(inspected.error());
        assertTrue(inspected.definitions().isEmpty());
        assertEquals(List.of("bean.NewBean", "model.NewEntity", "gone.Nothing"),
                inspected.notLoaded());
        assertEquals(Set.of("NewBean"), inspected.stereotypes());
        assertEquals(Set.of("NewEntity"), inspected.entities());
    }

    @Test
    void inspect_aLoadedClassWithNoNewBytesIsTheOneError() {
        // The daemon compiled it before asking, so bytes that are not on the
        // search path mean the path is wrong. Redefining the rest of the
        // change-set would report success over a class that never got them.
        String name = NothingDeclared.class.getName();

        DevLoopRedefiner.Inspection inspected = DevLoopRedefiner.inspect(
                List.of(name), Map.of(name, List.of(NothingDeclared.class)),
                List.of(Path.of("no", "such", "directory")));

        assertEquals("ERR kind=missing-class-file searched=1 message=" + name,
                inspected.error());
    }

    @Test
    void reply_carriesEveryFieldTheDaemonReadsAVerdictFrom() {
        // The daemon splits this line on whitespace and reads by name, so a
        // renamed or dropped field is a silently different answer rather than
        // a parse failure - which is why the whole line is asserted.
        DevLoopRedefiner.Inspection inspected = new DevLoopRedefiner.Inspection(
                List.of(), List.of("gone.Nothing"), 1, Set.of("Order"),
                Set.of("TaskService"), Set.of("NewBean"),
                Set.of("TaskListView"), null);
        DevLoopRedefiner.Applied applied = new DevLoopRedefiner.Applied(
                Set.of("TaskService"), Set.of("TaskRepository"),
                Set.of("TaskListView"), true, false, 4, 7);

        // hotswapAgent is read off this JVM, which has no agent on its
        // classpath.
        assertEquals(
                "OK redefined=0 notLoaded=1 dupes=1 completed=true"
                        + " pageReload=false entities=Order beans=TaskService"
                        + " proxied=TaskRepository structural=TaskService"
                        + " ui=TaskListView frontendImports=TaskListView"
                        + " hotswapAgent=false redefineMs=4 hotswapMs=7"
                        + " stereotypes=NewBean",
                DevLoopRedefiner.reply(inspected, applied));
    }

    /** The module's own test output, which is a real classpath directory. */
    private static Path testClasses() {
        try {
            return Path.of(DevLoopRedefinerTest.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A class file for a name nothing has loaded, carrying one annotation
     * descriptor in its constant pool - which is all these checks read.
     */
    private static void writeClass(Path root, String binaryName,
            String descriptor) throws IOException {
        Path file = root.resolve(binaryName.replace('.', '/') + ".class");
        Files.createDirectories(file.getParent());
        Files.write(file,
                withConstant(classBytes(NothingDeclared.class), descriptor));
    }

    @Test
    void frontend_carriesTheFieldsTheDaemonDecidesModeWith() {
        String reply = DevLoopRedefiner.frontend(null);

        // frontend= keeps its exact previous meaning and position, so a daemon
        // reading only that field is unaffected by the added ones.
        assertTrue(reply.startsWith("OK frontend=unknown"), reply);
        assertTrue(reply.contains(" mode="), reply);
        assertTrue(reply.contains(" themes="), reply);
        assertTrue(reply.contains(" agree=?"), reply);
    }

    @Test
    void frontendCheck_withoutAService_reportsItInTheProtocolVocabulary() {
        // Nothing is registered in a unit test, so the connector has to say so
        // in one line - the daemon reads "kind" and falls back to the log
        // rather than blocking on a reply that never comes.
        String reply = DevLoopRedefiner.frontendCheck("/tmp/a.ts");

        assertTrue(reply.startsWith("ERR kind=no-service"), reply);
    }

    @Test
    void relativeName_isTheForwardSlashedPathUnderTheFrontendRoot() {
        Path root = Paths.get("/p/src/main/frontend").toAbsolutePath();

        // A file below the root has a URL on the dev server; the name is that
        // path, always with forward slashes so it reads as a URL on Windows
        // too.
        assertEquals("views/hello.tsx", DevLoopRedefiner.relativeName(root,
                root.resolve("views/hello.tsx").toString()));
    }

    @Test
    void relativeName_isNullForAFileOutsideTheRootOrTheRootItself() {
        Path root = Paths.get("/p/src/main/frontend").toAbsolutePath();

        // The dev server's root is the frontend folder, so nothing above or
        // beside it has a URL - and failing an apply over a file the server was
        // never going to serve would be a worse answer than the truth.
        assertNull(DevLoopRedefiner.relativeName(root,
                Paths.get("/p/src/main/java/View.java").toAbsolutePath()
                        .toString()));
        // The root itself is not a module either.
        assertNull(DevLoopRedefiner.relativeName(root, root.toString()));
    }

    @Test
    void viteErrorMessage_readsTheReportOutOfVitesErrorPage() {
        // Vite answers a module it could not transform with an HTML page that
        // carries the failure as a JSON message its overlay renders. A line
        // break rides back as the separator the daemon splits on; a tab, only
        // ever indentation, flattens to a space.
        String body = "<html><script>{\"message\":\"Transform failed with 1"
                + " error:\\n[PARSE_ERROR] Expected `}`\\t^\"}</script></html>";

        String message = DevLoopRedefiner.viteErrorMessage(body, "any/url");

        // The unit separator ('\u001f') is what AppLog.SEGMENT is on the
        // daemon side, the only reader of this field.
        assertEquals(
                List.of("Transform failed with 1 error:",
                        "[PARSE_ERROR] Expected `}` ^"),
                List.of(message.split("\u001f")));
    }

    @Test
    void viteErrorMessage_fallsBackWhenThePageHasNoMessage() {
        // Best-effort and never load-bearing: the refusal is the verdict, so a
        // page whose shape has moved - or one with an empty message - still
        // fails the apply, just less precisely.
        assertEquals("the dev server could not compile my/module.ts",
                DevLoopRedefiner.viteErrorMessage("not an error page at all",
                        "my/module.ts"));
        assertEquals("the dev server could not compile my/module.ts",
                DevLoopRedefiner.viteErrorMessage("{\"message\":\"\"}",
                        "my/module.ts"));
    }
}
