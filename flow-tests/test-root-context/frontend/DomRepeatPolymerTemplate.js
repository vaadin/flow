import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class EmployeesList extends PolymerElement {
  static get is() {
    return 'employees-list'
  }

  static get template() {
    return html`
      <table>
        <tr>
            <th>Name</th>
            <th>Title</th>
            <th>Email</th>
        </tr>

        <template is="dom-repeat" items="[[employees]]">
            <tr on-click="handleClick" id="[[item.name]]">
                <td>{{item.name}}</td>
                <td>{{item.title}}</td>
                <td>{{item.email}}</td>
            </tr>
        </template>
    </table>

    <div id="eventIndex">[[eventIndex]]</div>
    <div id="repeatIndex">[[repeatIndex]]</div>
    `;
  }
}
customElements.define(EmployeesList.is, EmployeesList);
