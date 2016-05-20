package com.vaadin.client.hummingbird.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.dom.PolymerDomApiImpl.Polymer;
import com.vaadin.client.hummingbird.dom.PolymerDomApiImpl.UpdateStyles;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

public class GwtPolymerApiImplTest extends ClientEngineTestBase {

    @JsProperty(namespace = JsPackage.GLOBAL, name = "Polymer")
    private static native void setPolymer(Polymer polymer);

    static void setPolymerFull() {
        TestPolymer polymer = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        UpdateStyles updateStyles = WidgetUtil
                .crazyJsCast((JavaScriptObject.createFunction()));
        polymer.setUpdateStyles(updateStyles);

        setPolymer(polymer);
    }

    static void setPolymerMicro() {
        Polymer polymer = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        setPolymer(polymer);
    }

    static void clearPolymer() {
        setPolymer(null);
    }

    private void initTest() {
        clearPolymer();
        verifyPolymerLoaded(false);
        verifyPolymerMicro(false);
    }

    public void testPolymerMicroLoaded() {
        initTest();

        setPolymerMicro();

        verifyPolymerMicro(true);
        verifyPolymerLoaded(false);
    }

    public void testPolymerFullyLoaded() {
        initTest();

        setPolymerFull();

        verifyPolymerLoaded(true);
        verifyPolymerMicro(true);
    }

    private static void verifyPolymerLoaded(boolean loaded) {
        assertEquals(
                "Polymer should " + (loaded ? "" : "not ") + "be fully loaded",
                loaded, PolymerDomApiImpl.isPolymerFullyLoaded());
    }

    private static void verifyPolymerMicro(boolean loaded) {
        assertEquals(
                "Polymer micro should " + (loaded ? "" : "not ") + "be loaded",
                loaded, PolymerDomApiImpl.isPolymerMicroLoaded());
    }

}
