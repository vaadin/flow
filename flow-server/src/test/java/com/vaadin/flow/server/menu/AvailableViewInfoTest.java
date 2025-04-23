package com.vaadin.flow.server.menu;

import com.vaadin.flow.router.MenuData;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class AvailableViewInfoTest {

    MenuData menuData = new MenuData("title", 1.0, false, "icon", null);

    @Test
    public void testEquality() {
        Assert.assertEquals("Two instance created the same way are not equal",
                createInfo(true), createInfo(true));
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        var info = createInfo(true);

        var baos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(baos);
        oos.writeObject(info);
        oos.close();

        var ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
        var deserializedInfo = (AvailableViewInfo) ois.readObject();

        Assert.assertEquals("Serialized instance is not equal to origin", info,
                deserializedInfo);
    }

    private AvailableViewInfo createInfo(boolean withChild) {
        return new AvailableViewInfo("Title", new String[] { "role1" }, false,
                "route", false, true, menuData,
                withChild ? List.of(createInfo(false)) : List.of(),
                Map.of("param", RouteParamType.REQUIRED), false, "detail");
    }
}
