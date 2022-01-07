package com.vaadin.flow.spring.hillasecurity.endpoints;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.hilla.Endpoint;
import com.vaadin.flow.spring.hillasecurity.hillaform.Elements;
import com.vaadin.flow.spring.hillasecurity.hillaform.Elements.Options;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Endpoint
@AnonymousAllowed
public class ElementsEndpoint {

    public Elements getElements() {
        return new Elements();
    }

    public List<String> getOptions() {
        return Stream.of(Options.values()).map(Enum::toString)
                .collect(Collectors.toList());
    }

    public Elements saveElements(Elements item) {
        return item;
    }
}
