package com.vaadin.flow.server.frontend.scanner;

import java.util.stream.Collectors;

import org.junit.Test;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.CssClass1;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.CssClass2;

import static com.vaadin.flow.server.frontend.scanner.ScannerDependenciesTest.getFrontendDependencies;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ScannerCssTest {

    @Test
    public void should_visitCssImports() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(CssClass1.class, CssClass2.class);
        assertEquals(2, deps.getEndPoints().size());
        deps.getEndPoints().forEach(endPoint -> {
            assertEquals(4, endPoint.getCss().size());

            assertThat(endPoint.getCss().stream()
                    .map(CssData::toString).collect(Collectors.toList()),
                        containsInAnyOrder(
                            "value: ./foo.css",
                            "value: ./foo.css include:bar",
                            "value: ./foo.css id:bar",
                            "value: ./foo.css themefor:bar"));
        });
    }

    @Test
    public void should_gatherCssImportsInOrderPerClass() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(CssClass3.class);
        assertEquals(1, deps.getEndPoints().size());
        deps.getEndPoints().forEach(endPoint -> {
            assertEquals(4, endPoint.getCss().size());

            // verifies #6523 as sufficiently complex names can get mixed up
            assertThat(endPoint.getCss().stream()
                            .map(CssData::toString).collect(Collectors.toList()),
                    contains("value: ./foo.css",
                            "value: ./bar.css",
                            "value: ./foofoo.css",
                            "value: ./foobar.css"));
        });
    }

    @Test
    public void should_sumarizeCssImports() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(CssClass1.class, CssClass2.class);
        assertEquals(4, deps.getCss().size());
    }

    // Moved here from ScannerTestComponents. Otherwise this would affect
    // NodeUpdateImportsTest.java
    @Route("css-route-3")
    @CssImport("./foo.css")
    @CssImport(value = "./bar.css")
    @CssImport(value = "./foofoo.css")
    @CssImport(value = "./foobar.css")
    public static class CssClass3 {
    }

}
