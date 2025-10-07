import { LitElement, html } from 'lit';
import { customElement } from 'lit/decorators.js';
import { Greetings } from 'Frontend/views/another.js';

@customElement('lit-view')
export class LitView extends LitElement {
  render() {
    const greetings = new Greetings();
    const hello = greetings.sayHello('John Doe');

    return html` <div><p>Greetings from test web component: ${hello}</p></div> `;
  }
}
