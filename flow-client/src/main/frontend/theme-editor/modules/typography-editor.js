import { EditorModule } from './editor-module.js';
import '../property-editors/select-editor.js';
import './preset-picker.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
class Typography extends EditorModule {
  static get template() {
    return html`
    <style include="shared-editor-module-styles">
      .font {
        display: flex;
      }

      .font-size {
        width: 6em;
        margin-right: 0.5em;
      }

      .line-height {
        flex: auto;
      }

      details {
        margin-top: 2em;
      }
    </style>

    <select-editor label="Font family" name="--lumo-font-family" class="font-family">
      <template>
        <vaadin-list-box>
          <vaadin-item value="">System default</vaadin-item>
          <vaadin-item value="Cambria, &quot;Hoefler Text&quot;, Utopia, &quot;Liberation Serif&quot;, &quot;Nimbus Roman No9 L Regular&quot;, Times, &quot;Times New Roman&quot;, serif">Times New Roman</vaadin-item>
          <vaadin-item value="Constantia, &quot;Lucida Bright&quot;, Lucidabright, &quot;Lucida Serif&quot;, Lucida, &quot;DejaVu Serif&quot;, &quot;Bitstream Vera Serif&quot;, &quot;Liberation Serif&quot;, Georgia, serif">Georgia</vaadin-item>
          <vaadin-item value="&quot;Palatino Linotype&quot;, Palatino, Palladio, &quot;URW Palladio L&quot;, &quot;Book Antiqua&quot;, Baskerville, &quot;Bookman Old Style&quot;, &quot;Bitstream Charter&quot;, &quot;Nimbus Roman No9 L&quot;, Garamond, &quot;Apple Garamond&quot;, &quot;ITC Garamond Narrow&quot;, &quot;New Century Schoolbook&quot;, &quot;Century Schoolbook&quot;, &quot;Century Schoolbook L&quot;, Georgia, serif">Garamond</vaadin-item>
          <vaadin-item value="Frutiger, &quot;Frutiger Linotype&quot;, Univers, Calibri, &quot;Gill Sans&quot;, &quot;Gill Sans MT&quot;, &quot;Myriad Pro&quot;, Myriad, &quot;DejaVu Sans Condensed&quot;, &quot;Liberation Sans&quot;, &quot;Nimbus Sans L&quot;, Tahoma, Geneva, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif">Helvetica</vaadin-item>
          <vaadin-item value="Corbel, &quot;Lucida Grande&quot;, &quot;Lucida Sans Unicode&quot;, &quot;Lucida Sans&quot;, &quot;DejaVu Sans&quot;, &quot;Bitstream Vera Sans&quot;, &quot;Liberation Sans&quot;, Verdana, &quot;Verdana Ref&quot;, sans-serif">Verdana</vaadin-item>
          <vaadin-item value="&quot;Segoe UI&quot;, Candara, &quot;Bitstream Vera Sans&quot;, &quot;DejaVu Sans&quot;, &quot;Bitstream Vera Sans&quot;, &quot;Trebuchet MS&quot;, Verdana, &quot;Verdana Ref&quot;, sans-serif">Trebuchet</vaadin-item>
          <vaadin-item value="Impact, Haettenschweiler, &quot;Franklin Gothic Bold&quot;, Charcoal, &quot;Helvetica Inserat&quot;, &quot;Bitstream Vera Sans Bold&quot;, &quot;Arial Black&quot;, sans-serif">Impact</vaadin-item>
          <vaadin-item value="Consolas, &quot;Andale Mono WT&quot;, &quot;Andale Mono&quot;, &quot;Lucida Console&quot;, &quot;Lucida Sans Typewriter&quot;, &quot;DejaVu Sans Mono&quot;, &quot;Bitstream Vera Sans Mono&quot;, &quot;Liberation Mono&quot;, &quot;Nimbus Mono L&quot;, Monaco, &quot;Courier New&quot;, Courier, monospace">Monospace</vaadin-item>
        </vaadin-list-box>
      </template>
    </select-editor>

    <div class="font">
      <preset-picker label="Font-size" class="font-size">
        <template>
          <vaadin-list-box>
            <preset-item value="xs" properties="{
              &quot;--lumo-font-size&quot;: &quot;1rem&quot;,
              &quot;--lumo-font-size-xxxl&quot;: &quot;1.375rem&quot;,
              &quot;--lumo-font-size-xxl&quot;: &quot;1.125rem&quot;,
              &quot;--lumo-font-size-xl&quot;: &quot;1rem&quot;,
              &quot;--lumo-font-size-l&quot;: &quot;0.875rem&quot;,
              &quot;--lumo-font-size-m&quot;: &quot;0.75rem&quot;,
              &quot;--lumo-font-size-s&quot;: &quot;0.6875rem&quot;,
              &quot;--lumo-font-size-xs&quot;: &quot;0.625rem&quot;,
              &quot;--lumo-font-size-xxs&quot;: &quot;0.625rem&quot;
            }">
              XS
            </preset-item>
            <preset-item value="s" properties="{
              &quot;--lumo-font-size&quot;: &quot;1rem&quot;,
              &quot;--lumo-font-size-xxxl&quot;: &quot;1.75rem&quot;,
              &quot;--lumo-font-size-xxl&quot;: &quot;1.375rem&quot;,
              &quot;--lumo-font-size-xl&quot;: &quot;1.125rem&quot;,
              &quot;--lumo-font-size-l&quot;: &quot;1rem&quot;,
              &quot;--lumo-font-size-m&quot;: &quot;0.875rem&quot;,
              &quot;--lumo-font-size-s&quot;: &quot;0.8125rem&quot;,
              &quot;--lumo-font-size-xs&quot;: &quot;0.75rem&quot;,
              &quot;--lumo-font-size-xxs&quot;: &quot;0.6875rem&quot;
            }">
              S
            </preset-item>
            <preset-item value="" properties="{
              &quot;--lumo-font-size&quot;: null,
              &quot;--lumo-font-size-xxxl&quot;: null,
              &quot;--lumo-font-size-xxxl&quot;: null,
              &quot;--lumo-font-size-xxl&quot;: null,
              &quot;--lumo-font-size-xl&quot;: null,
              &quot;--lumo-font-size-l&quot;: null,
              &quot;--lumo-font-size-m&quot;: null,
              &quot;--lumo-font-size-s&quot;: null,
              &quot;--lumo-font-size-xs&quot;: null,
              &quot;--lumo-font-size-xxs&quot;: null
            }">
              M
            </preset-item>
            <preset-item value="l" properties="{
              &quot;--lumo-font-size&quot;: &quot;1rem&quot;,
              &quot;--lumo-font-size-xxxl&quot;: &quot;3rem&quot;,
              &quot;--lumo-font-size-xxl&quot;: &quot;2.25rem&quot;,
              &quot;--lumo-font-size-xl&quot;: &quot;1.75rem&quot;,
              &quot;--lumo-font-size-l&quot;: &quot;1.375rem&quot;,
              &quot;--lumo-font-size-m&quot;: &quot;1.125rem&quot;,
              &quot;--lumo-font-size-s&quot;: &quot;1rem&quot;,
              &quot;--lumo-font-size-xs&quot;: &quot;0.875rem&quot;,
              &quot;--lumo-font-size-xxs&quot;: &quot;0.8125rem&quot;
            }">
              L
            </preset-item>
          </vaadin-list-box>
        </template>
      </preset-picker>

      <preset-picker label="Line height" class="line-height">
        <template>
          <vaadin-list-box>
            <preset-item value="s" properties="{
                &quot;--lumo-line-height-m&quot;: &quot;1.4&quot;,
                &quot;--lumo-line-height-s&quot;: &quot;1.2&quot;,
                &quot;--lumo-line-height-xs&quot;: &quot;1.1&quot;
            }">
              Tight
            </preset-item>
            <preset-item value="" properties="{
                &quot;--lumo-line-height-m&quot;: null,
                &quot;--lumo-line-height-s&quot;: null,
                &quot;--lumo-line-height-xs&quot;: null
            }">
              Normal
            </preset-item>
            <preset-item value="loose" properties="{
                &quot;--lumo-line-height-m&quot;: &quot;1.8&quot;,
                &quot;--lumo-line-height-s&quot;: &quot;1.5&quot;,
                &quot;--lumo-line-height-xs&quot;: &quot;1.3&quot;
            }">
              Loose
            </preset-item>
          </vaadin-list-box>
        </template>
      </preset-picker>
    </div>

    <details>
      <summary>Advanced</summary>

      <table>
        <thead>
          <tr>
            <th>Font family</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><property-editor name="--lumo-font-family"></property-editor></td>
          </tr>
        </tbody>
      </table>

      <table>
        <thead>
          <tr>
            <th colspan="2">Font size</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th>3X large</th>
            <td><property-editor name="--lumo-font-size-xxxl"></property-editor></td>
          </tr>
          <tr>
            <th>2X large</th>
            <td><property-editor name="--lumo-font-size-xxl"></property-editor></td>
          </tr>
          <tr>
            <th>X large</th>
            <td><property-editor name="--lumo-font-size-xl"></property-editor></td>
          </tr>
          <tr>
            <th>Large</th>
            <td><property-editor name="--lumo-font-size-l"></property-editor></td>
          </tr>
          <tr>
            <th>Medium</th>
            <td><property-editor name="--lumo-font-size-m"></property-editor></td>
          </tr>
          <tr>
            <th>Small</th>
            <td><property-editor name="--lumo-font-size-s"></property-editor></td>
          </tr>
          <tr>
            <th>X small</th>
            <td><property-editor name="--lumo-font-size-xs"></property-editor></td>
          </tr>
          <tr>
            <th>2X small</th>
            <td><property-editor name="--lumo-font-size-xxs"></property-editor></td>
          </tr>
        </tbody>
      </table>

      <table>
        <thead>
          <tr>
            <th colspan="2">Line height</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th>Medium</th>
            <td><property-editor name="--lumo-line-height-m"></property-editor></td>
          </tr>
          <tr>
            <th>Small</th>
            <td><property-editor name="--lumo-line-height-s"></property-editor></td>
          </tr>
          <tr>
            <th>X small</th>
            <td><property-editor name="--lumo-line-height-xs"></property-editor></td>
          </tr>
        </tbody>
      </table>
    </details>
`;
  }

  static get is() {
    return 'typography-editor';
  }

  ready() {
    super.ready();

    this.shadowRoot.querySelector('.font-family').items = [
      {label: "System default", value: ""},
      {label: "Helvetica", value: "Helvetica Neue, Arial, Helvetica, sans-serif"},
      {label: "Verdana", value: "Verdana, Geneva, sans-serif"}
    ];

    // document.addEventListener(LumoEditor.PROPERTY_CHANGED, e => {
    //   var entry = e.detail;
    // });
  }
}

customElements.define(Typography.is, Typography);
