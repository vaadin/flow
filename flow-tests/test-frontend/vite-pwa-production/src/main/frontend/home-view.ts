class HomeView extends HTMLElement {
  connectedCallback() {
    this.innerHTML = `<h1>Home Page</h1>`;
  }
}

customElements.define('home-view', HomeView);
