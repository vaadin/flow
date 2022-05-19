package com.vaadin.flow.spring;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class RootMappedConditionTest {

    @Test
    public void lookupProperty() {
        assertUrlMapping("url-mapping");
        assertUrlMapping("urlmapping");
        assertUrlMapping("urlMapping");
        assertUrlMapping("URL-mapping");
        assertUrlMapping("URLMAPPING");
    }

    private void assertUrlMapping(String key) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("vaadin." + key, "abc");
        Assert.assertEquals("abc",
                RootMappedCondition.getUrlMapping(environment));
    }
}
