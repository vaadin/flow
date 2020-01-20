import com.vaadin.flow.server.connect.Endpoint;

/**
 * A test class.
 */
@Endpoint
public class MyEndpoint {

    /**
     * Foo endpoint.
     * 
     * @param bar
     */
    public void foo(String bar) {
    }

    /**
     * Baz endpoint.
     * 
     * @param baz
     * @return
     */
    public String bar(String baz) {
        return baz;
    }

}
