public interface HasTitle extends HasElement {
    default setTitle(String title) {
        getElement().setAttribute("title", title);
    }
}
