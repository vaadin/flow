package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.Test;

import com.vaadin.flow.testutil.PhantomJSTest;

/**
 * See {@link DependenciesLoadingAnnotationsIT} for more details about the test.
 * 
 * @author Vaadin Ltd.
 * @see DependenciesLoadingAnnotationsIT
 */
@SuppressWarnings("Duplicates")
public class DependenciesLoadingPageApiIT extends PhantomJSTest {

    @Test
    public void dependenciesLoadedAsExpected() {
        open();
        DependenciesLoadingAnnotationsIT.runOnOpenPage(this);
    }
}
