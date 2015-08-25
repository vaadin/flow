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
                    getRootNode().enqueueRpc(input.getNode(), "$0.focus()",
                            input);
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Show alert");
                addEventListener("click", e -> {
                    getRootNode().enqueueRpc(getRootNode(), "window.alert($0)",
                            "Hello");
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Log stuff");
                addEventListener("click", e -> {
                    getRootNode().enqueueRpc(getRootNode(),
                            "console.log($0, $1)", "Foobar", input);
                });
            }
        });

        getElement().appendChild(new Element("button") {
            {
                setTextContent("Log window.vaadin property names");
                addEventListener("click", e -> {
                    getRootNode().enqueueRpc(getRootNode(),
                            "console.log(Object.getOwnPropertyNames(window.vaadin))");
                });
            }
        });

    }

}
