import { EditorModule } from './editor-module.js';
import '../property-editors/select-editor.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { LumoEditor } from '../lumo-editor.js';
const $_documentContainer = document.createElement('template');

class Style extends EditorModule {
  static get template() {
    return html`
      <style include="shared-editor-module-styles"></style>

      <select-editor label="Roundness" name="--lumo-border-radius">
        <template>
          <vaadin-list-box>
            <vaadin-item value="0px">Sharp</vaadin-item>
            <hr />
            <vaadin-item value="0.125em">Extra small</vaadin-item>
            <vaadin-item value="">Small</vaadin-item>
            <vaadin-item value="0.5em">Medium</vaadin-item>
            <vaadin-item value="0.75em">Large</vaadin-item>
            <vaadin-item value="calc(var(--lumo-size-m) / 2)">Round</vaadin-item>
          </vaadin-list-box>
        </template>
      </select-editor>

      <details>
        <summary>Advanced</summary>

        <table>
          <tbody>
            <tr>
              <th>Radius</th>
              <td><property-editor name="--lumo-border-radius"></property-editor></td>
            </tr>
            <tr>
              <th>Radius S</th>
              <td><property-editor name="--lumo-border-radius-s"></property-editor></td>
            </tr>
            <tr>
              <th>Radius M</th>
              <td><property-editor name="--lumo-border-radius-m"></property-editor></td>
            </tr>
            <tr>
              <th>Radius L</th>
              <td><property-editor name="--lumo-border-radius-l"></property-editor></td>
            </tr>
          </tbody>
        </table>
      </details>
    `;
  }

  static get is() {
    return 'style-editor';
  }
}

customElements.define(Style.is, Style);
