package com.vaadin.fusion.generator.typescript;

import org.junit.Assert;
import org.junit.Test;

public class CodeGeneratorUtilsTest {
    @Test
    public void should_escapeWhiteSpaceInFilePath() {
        String filePath = "/my project/foo/bar bar";
        Object result = CodeGeneratorUtils.escapeFilePath(filePath);
        Assert.assertEquals("/my%20project/foo/bar%20bar", result);
    }
}
