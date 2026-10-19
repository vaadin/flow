import { html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';
import styles from './hello-world-view.css?inline';

@customElement('hello-world-view')
export class HelloWorldView extends LitElement {
  name: string = '';

  static styles = [styles];

  render() {
    return html` <label>Your name <input type="text" @change="${this.nameChanged}" /></label> `;
  }
  nameChanged(e: Event) {
    this.name = (e.target as HTMLInputElement).value;
  }
}
