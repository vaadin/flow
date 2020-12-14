import '@vaadin/vaadin-text-field';
import { customElement, html, LitElement } from 'lit-element';
import styles from './hello-world-view.css';


@customElement('hello-world-view')
export class HelloWorldView extends LitElement {
  name: string = '';

  static styles = [styles];

  render() {
    return html`
      <vaadin-text-field label="Your name" @value-changed="${this.nameChanged}"></vaadin-text-field>
    `;
  }
  nameChanged(e: CustomEvent) {
    this.name = e.detail.value;
  }
}
