package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.IgnoreNPM;

/**
 * See {@link DependenciesLoadingAnnotationsIT} for more details about the test.
 * Test runs and performs the same checks as
 * {@link DependenciesLoadingAnnotationsIT}, but this test opens a different
 * page to test, that's why the class exists and needed.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @see DependenciesLoadingAnnotationsIT
 */
@Category(IgnoreNPM.class)
public class DependenciesLoadingPageApiIT
        extends DependenciesLoadingAnnotationsIT {
}
