package com.vaadin.hummingbird.testcategory;

/**
 * Tests which take more than 0.5s to run should be annotated with @
 * {@code @Category(SlowTests.class)} and will be excluded by default from the
 * test suite.
 *
 * @author Vaadin Ltd
 */
public interface SlowTests {

}
