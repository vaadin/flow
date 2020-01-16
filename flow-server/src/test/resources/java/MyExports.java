import com.vaadin.flow.server.connect.Export;

/**
 * A test class.
 */
@Export
public class MyExports {

    /**
     * Foo export.
     * 
     * @param bar
     */
    public void foo(String bar) {
    }

    /**
     * Baz export.
     * 
     * @param baz
     * @return
     */
    public String bar(String baz) {
        return baz;
    }

}
