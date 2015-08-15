package com.vaadin.hummingbird.kernel;

import java.util.Collections;

public class StaticTextTemplate extends BoundElementTemplate {

    public StaticTextTemplate(String content) {
        super("#text", Collections.emptyList(),
                Collections.singletonMap("content", content),
                Collections.emptyList());
    }

}
