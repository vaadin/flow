import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class LoginView extends PolymerElement {
  static get template() {
    return html`
        <form action="login" method="POST">
          Username:<br>
          <input type="text" name="username" placeholder="user@vaadin.com"><br>
          Password:<br>
          <input type="password" name="password" placeholder="password"><br>
          <input type="submit" value="Submit">
        </form>
    `;
  }

  static get is() {
    return 'login-view';
  }
}
customElements.define(LoginView.is, LoginView);
