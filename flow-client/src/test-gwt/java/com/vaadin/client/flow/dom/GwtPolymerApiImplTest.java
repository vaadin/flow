package com.vaadin.client.flow.dom;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.dom.PolymerDomApiImpl.Polymer;
import com.vaadin.client.flow.util.NativeFunction;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

public class GwtPolymerApiImplTest extends ClientEngineTestBase {

    @JsProperty(namespace = JsPackage.GLOBAL, name = "Polymer")
    private static native void setPolymer(Polymer polymer);

    static void setPolymerFull() {
        setPolymerMicro();
    }

    static void setPolymerMicro() {
        NativeFunction function = NativeFunction
                .create("return { 'version': '1.9.1' };");
        setPolymer(WidgetUtil.crazyJsCast(function.call(null)));
    }

    static void clearPolymer() {
        setPolymer(null);
    }

    private void initTest() {
        clearPolymer();
        verifyPolymerMicro(false);
    }

    public void testPolymerMicroLoaded() {
        initTest();

        setPolymerMicro();

        verifyPolymerMicro(true);
    }

    public void testPolymer2() {
        initTest();

        NativeFunction function = NativeFunction
                .create("return { 'version': '2.0.0' };");
        setPolymer(WidgetUtil.crazyJsCast(function.call(null)));

        verifyPolymerMicro(false);
    }

    private static void verifyPolymerMicro(boolean loaded) {
        assertEquals(
                "Polymer micro should " + (loaded ? "" : "not ") + "be loaded",
                loaded, PolymerDomApiImpl.isPolymerMicroLoaded());
    }

}
