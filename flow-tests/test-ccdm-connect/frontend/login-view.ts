import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-if.js';
import * as connectServices from './generated/ConnectServices';

class LoginView extends PolymerElement {

  private isUserLoggedIn: boolean = false;
  // @ts-ignore
  private currentUserName: string = '';

  static get template() {
    return html`
        <template is="dom-if" if="{{!isUserLoggedIn}}">
          <form action="login" method="POST">
            Username:<br>
            <input id="username" type="text" name="username" placeholder="user@vaadin.com"><br>
            Password:<br>
            <input id="password" type="password" name="password" placeholder="password"><br>
            <input id="submit" type="submit" value="Submit">
          </form>
        </template>
        <template is="dom-if" if="{{isUserLoggedIn}}">
          Hello, [[currentUserName]]
        </template>
    `;
  }

  async connectedCallback() {
    super.connectedCallback();
    await this.checkLoggedIn();
  }

  static get is() {
    return 'login-view';
  }

  private async checkLoggedIn() {
    this.isUserLoggedIn = await connectServices.isUserLoggedIn();
    if (this.isUserLoggedIn) {
      this.currentUserName = await connectServices.getCurrentUserName();
    }
  }

}
customElements.define(LoginView.is, LoginView);
