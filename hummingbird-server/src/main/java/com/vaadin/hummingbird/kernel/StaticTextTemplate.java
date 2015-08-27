package com.vaadin.hummingbird.kernel;

import java.util.Collections;

public class StaticTextTemplate extends BoundElementTemplate {

    private String content;

    public StaticTextTemplate(String content) {
        super("#text", Collections.emptyList(),
                Collections.singletonMap("content", content),
                Collections.emptyList(), null);
        this.content = content;
    }

    public String getContent() {
        return content;
    }

}
