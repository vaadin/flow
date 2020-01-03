import com.vaadin.flow.server.connect.VaadinService;

/**
 * A test class.
 */
@VaadinService
public class MyVaadinServices {

    /**
     * Foo service.
     * 
     * @param bar
     */
    public void foo(String bar) {
    }

    /**
     * Baz service.
     * 
     * @param baz
     * @return
     */
    public String bar(String baz) {
        return baz;
    }

}
