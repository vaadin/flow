package com.vaadin.base.devserver;

import java.util.Locale;
import java.util.ResourceBundle;

import com.vaadin.flow.component.UI;

public class Translator {

    public static String _T(String key) {
        UI ui = UI.getCurrent();
        ResourceBundle bundle = getBundle(ui.getLocale());
        String lookupKey = key.replaceAll(" ", "-").replaceAll("[^a-zA-Z0-9-_]",
                "").replaceAll("(-*)$","");
        if (bundle.containsKey(lookupKey)) {
            return bundle.getString(lookupKey);
        } else {
            return key;
        }
    }

    private static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("translations", locale);
    }
}
