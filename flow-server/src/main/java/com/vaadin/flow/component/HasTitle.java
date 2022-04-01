public interface HasTitle extends HasElement {
    default setTitle(String title) {
        this.setAttribute("title", title);
    }
}
