package com.vaadin.flow.server.communication;

import org.junit.Assert;
import org.junit.Test;

public class WebComponentBootstrapHandlerTest {

    @Test
    public void inlineHTML_basicText() {
        Assert.assertEquals("var foo='bar';",
                WebComponentBootstrapHandler.inlineHTML("var foo='bar';"));
    }

    @Test
    public void inlineHTML_multiline() {
        Assert.assertEquals("var foo='bar';var foo2=2;",
                WebComponentBootstrapHandler
                        .inlineHTML("var foo='bar';\nvar foo2=2;"));
    }

    @Test
    public void inlineHTML_singleLineComments() {
        Assert.assertEquals("var foo='bar';/*var foo2=2;*/var foo3='baz';",
                WebComponentBootstrapHandler.inlineHTML( //
                        "var foo='bar';\n" //
                                + "//var foo2=2;\n" //
                                + "var foo3='baz';\n"));
    }

    @Test
    public void inlineHTML_two_singleLineComments() {
        Assert.assertEquals(
                "var foo='bar';/*var foo2=2;*//* Second row*/var foo3='baz';",
                WebComponentBootstrapHandler.inlineHTML( //
                        "var foo='bar';\n" //
                                + "//var foo2=2;\n" //
                                + "// Second row\n" //
                                + "var foo3='baz';\n"));
    }

    @Test
    public void inlineHTML_multiLineComments() {
        Assert.assertEquals("var foo='bar';/*var foo2=2;*/var foo3='baz';",
                WebComponentBootstrapHandler.inlineHTML( //
                        "var foo='bar';\n" //
                                + "/*var foo2=2;*/\n" //
                                + "var foo3='baz';\n"));
    }

    @Test
    public void inlineHTML_two_multiLineComments() {
        Assert.assertEquals(
                "var foo='bar';/*var foo2=2;*//* Second row*/var foo3='baz';",
                WebComponentBootstrapHandler.inlineHTML( //
                        "var foo='bar';\n" //
                                + "/*var foo2=2;*/\n" //
                                + "/* Second row*/\n" //
                                + "var foo3='baz';\n"));
    }

}
