package com.vaadin.hummingbird.namespace;

import org.junit.Assert;
import org.junit.Test;

public class ElementDataNamespaceTest
        extends AbstractNamespaceTest<ElementDataNamespace> {
    private final ElementDataNamespace namespace = createNamespace();

    @Test
    public void testSetGetTag() {
        Assert.assertNull("Tag should initially be null", namespace.getTag());

        namespace.setTag("myTag");

        Assert.assertEquals("myTag", namespace.getTag());
    }
}
