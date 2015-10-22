package hummingbird;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUI;

public class RpcExamples extends AbstractTestUI {
    @Override
    protected void setup(VaadinRequest request) {
        Element input = new Element("input");

        getElement().appendChild(input);

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Focus input");
                addEventListener("click", e -> {
                    getRoot().getRootNode().enqueueRpc(input.getNode(),
                            "$0.focus()", input);
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Show alert");
                addEventListener("click", e -> {
                    getRoot().getRootNode().enqueueRpc(getRoot().getRootNode(),
                            "window.alert($0)", "Hello");
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Log stuff");
                addEventListener("click", e -> {
                    getRoot().getRootNode().enqueueRpc(getRoot().getRootNode(),
                            "console.log($0, $1)", "Foobar", input);
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Log window.vaadin.framework property names");
                addEventListener("click", e -> {
                    getRoot().getRootNode().enqueueRpc(getRoot().getRootNode(),
                            "console.log(Object.getOwnPropertyNames(window.vaadin.framework))");
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Send arrays");
                addEventListener("click", e -> {
                    // Define as object to avoid interpreting as varargs
                    getRoot().getRootNode().enqueueRpc(getRoot().getRootNode(),
                            "console.log($0, $1)",
                            new String[] { "Lorem", "Ipsum" },
                            new int[] { 1, 2 });
                });
            }
        });

    }

}
