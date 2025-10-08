import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@vaadin/vaadin-grid';
import '@vaadin/vaadin-grid/vaadin-grid-selection-column';

class GridColumns extends PolymerElement {
  static get template() {
    return html`
      <vaadin-grid aria-label="Basic example" items="[[users]]" column-reordering-allowed multi-sort>
        <vaadin-grid-selection-column auto-select></vaadin-grid-selection-column>

        <vaadin-grid-column width="9em">
          <template class="header">
            <vaadin-grid-sorter path="firstName">First Name</vaadin-grid-sorter>
          </template>
          <template>[[item.firstName]]</template>
          <template class="footer">First Name</template>
        </vaadin-grid-column>

        <vaadin-grid-column width="9em">
          <template class="header">
            <vaadin-grid-sorter path="lastName">Last Name</vaadin-grid-sorter>
          </template>
          <template>[[item.lastName]]</template>
          <template class="footer">Last Name</template>
        </vaadin-grid-column>

        <vaadin-grid-column width="15em" flex-grow="2">
          <template class="header">
            <vaadin-grid-sorter path="address.street">Address</vaadin-grid-sorter>
          </template>
          <template>[[item.address.street]], [[item.address.city]]</template>
          <template class="footer">Address</template>
        </vaadin-grid-column>
      </vaadin-grid>
    `;
  }

  static get is() {
    return 'grid-columns';
  }
  static get properties() {
    return {
      users: {
        type: Array,
        value: [
          {
            firstName: 'John',
            lastName: 'Short',
            address: { street: 'Homestreet 1', city: 'Boston' }
          },
          {
            firstName: 'Lea',
            lastName: 'Green',
            address: { street: 'Faraway 22', city: 'Cairo' }
          }
        ]
      }
    };
  }
}

customElements.define(GridColumns.is, GridColumns);
