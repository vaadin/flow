import { EditorModule } from './editor-module.js';
import '../property-editors/select-editor.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { LumoEditor } from '../lumo-editor.js';
const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<dom-module id="button--gradient" theme-for="vaadin-button">
  <template>
    <style>
      :host([theme~="gradient"]:not([theme~="tertiary"])) {
        background-image: linear-gradient(var(--lumo-tint-5pct), var(--lumo-shade-5pct));
        box-shadow: inset 0 0 0 1px var(--lumo-contrast-20pct);
      }

      :host([theme~="gradient"]:not([theme~="tertiary"]):not([theme~="primary"]):not([theme~="error"]):not([theme~="success"])) {
        color: var(--lumo-body-text-color);
      }

      :host([theme~="primary"][theme~="gradient"]) {
        text-shadow: 0 -1px 0 var(--lumo-shade-20pct);
      }
    </style>
  </template>
</dom-module><dom-module id="text-field--border" theme-for="vaadin-text-field">
  <template>
    <style>
      :host([theme~="border"]) [part="input-field"] {
        box-shadow: inset 0 0 0 1px var(--lumo-contrast-30pct);
        background-color: var(--lumo-base-color);
      }

      :host([theme~="border"][invalid]) [part="input-field"] {
        box-shadow: inset 0 0 0 1px var(--lumo-error-color);
      }
    </style>
  </template>
</dom-module>`;

document.head.appendChild($_documentContainer.content);
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

      <select-editor label="Button style" name="style-module:button">
        <template>
          <vaadin-list-box>
            <vaadin-item value="">Fill</vaadin-item>
            <vaadin-item value="gradient">Border &amp; gradient</vaadin-item>
          </vaadin-list-box>
        </template>
      </select-editor>

      <select-editor label="Text field style" name="style-module:text-field">
        <template>
          <vaadin-list-box>
            <vaadin-item value="">Fill</vaadin-item>
            <vaadin-item value="border">Border</vaadin-item>
          </vaadin-list-box>
        </template>
      </select-editor>
    `;
  }

  static get is() {
    return 'style-editor';
  }

  ready() {
    super.ready();

    let textfields = Array.from(document.querySelectorAll('vaadin-text-field'));
    textfields = textfields.concat(Array.from(document.querySelectorAll('vaadin-select')));
    textfields = textfields.concat(Array.from(document.querySelectorAll('vaadin-combo-box')));
    textfields = textfields.concat(Array.from(document.querySelectorAll('vaadin-date-picker')));

    let buttons = Array.from(document.querySelectorAll('vaadin-button'));

    document.addEventListener(LumoEditor.PROPERTY_CHANGED, (e) => {
      var entry = e.detail;

      for (var prop in entry.properties) {
        if (prop.indexOf('style-module') > -1) {
          var value = entry.properties[prop] || '';

          if (prop.indexOf('text-field') > -1) {
            textfields.forEach((tf) => {
              tf.setAttribute('theme', value);
            });
          } else if (prop.indexOf('button') > -1) {
            buttons.forEach((b) => {
              if (!value) {
                b.setAttribute('theme', b.__originalTheme);
              } else {
                b.__originalTheme = b.__originalTheme || b.getAttribute('theme');
                b.setAttribute('theme', b.__originalTheme + ' ' + value);
              }
            });
          }
        }
      }
    });
  }
}

customElements.define(Style.is, Style);
