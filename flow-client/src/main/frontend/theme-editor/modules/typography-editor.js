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
            <vaadin-item
              value='Cambria, "Hoefler Text", Utopia, "Liberation Serif", "Nimbus Roman No9 L Regular", Times, "Times New Roman", serif'
              >Times New Roman</vaadin-item
            >
            <vaadin-item
              value='Constantia, "Lucida Bright", Lucidabright, "Lucida Serif", Lucida, "DejaVu Serif", "Bitstream Vera Serif", "Liberation Serif", Georgia, serif'
              >Georgia</vaadin-item
            >
            <vaadin-item
              value='"Palatino Linotype", Palatino, Palladio, "URW Palladio L", "Book Antiqua", Baskerville, "Bookman Old Style", "Bitstream Charter", "Nimbus Roman No9 L", Garamond, "Apple Garamond", "ITC Garamond Narrow", "New Century Schoolbook", "Century Schoolbook", "Century Schoolbook L", Georgia, serif'
              >Garamond</vaadin-item
            >
            <vaadin-item
              value='Frutiger, "Frutiger Linotype", Univers, Calibri, "Gill Sans", "Gill Sans MT", "Myriad Pro", Myriad, "DejaVu Sans Condensed", "Liberation Sans", "Nimbus Sans L", Tahoma, Geneva, "Helvetica Neue", Helvetica, Arial, sans-serif'
              >Helvetica</vaadin-item
            >
            <vaadin-item
              value='Corbel, "Lucida Grande", "Lucida Sans Unicode", "Lucida Sans", "DejaVu Sans", "Bitstream Vera Sans", "Liberation Sans", Verdana, "Verdana Ref", sans-serif'
              >Verdana</vaadin-item
            >
            <vaadin-item
              value='"Segoe UI", Candara, "Bitstream Vera Sans", "DejaVu Sans", "Bitstream Vera Sans", "Trebuchet MS", Verdana, "Verdana Ref", sans-serif'
              >Trebuchet</vaadin-item
            >
            <vaadin-item
              value='Impact, Haettenschweiler, "Franklin Gothic Bold", Charcoal, "Helvetica Inserat", "Bitstream Vera Sans Bold", "Arial Black", sans-serif'
              >Impact</vaadin-item
            >
            <vaadin-item
              value='Consolas, "Andale Mono WT", "Andale Mono", "Lucida Console", "Lucida Sans Typewriter", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Liberation Mono", "Nimbus Mono L", Monaco, "Courier New", Courier, monospace'
              >Monospace</vaadin-item
            >
          </vaadin-list-box>
        </template>
      </select-editor>

      <div class="font">
        <preset-picker label="Font-size" class="font-size">
          <template>
            <vaadin-list-box>
              <preset-item
                value="xs"
                properties='{
              "--lumo-font-size": "1rem",
              "--lumo-font-size-xxxl": "1.375rem",
              "--lumo-font-size-xxl": "1.125rem",
              "--lumo-font-size-xl": "1rem",
              "--lumo-font-size-l": "0.875rem",
              "--lumo-font-size-m": "0.75rem",
              "--lumo-font-size-s": "0.6875rem",
              "--lumo-font-size-xs": "0.625rem",
              "--lumo-font-size-xxs": "0.625rem"
            }'
              >
                XS
              </preset-item>
              <preset-item
                value="s"
                properties='{
              "--lumo-font-size": "1rem",
              "--lumo-font-size-xxxl": "1.75rem",
              "--lumo-font-size-xxl": "1.375rem",
              "--lumo-font-size-xl": "1.125rem",
              "--lumo-font-size-l": "1rem",
              "--lumo-font-size-m": "0.875rem",
              "--lumo-font-size-s": "0.8125rem",
              "--lumo-font-size-xs": "0.75rem",
              "--lumo-font-size-xxs": "0.6875rem"
            }'
              >
                S
              </preset-item>
              <preset-item
                value=""
                properties='{
              "--lumo-font-size": null,
              "--lumo-font-size-xxxl": null,
              "--lumo-font-size-xxxl": null,
              "--lumo-font-size-xxl": null,
              "--lumo-font-size-xl": null,
              "--lumo-font-size-l": null,
              "--lumo-font-size-m": null,
              "--lumo-font-size-s": null,
              "--lumo-font-size-xs": null,
              "--lumo-font-size-xxs": null
            }'
              >
                M
              </preset-item>
              <preset-item
                value="l"
                properties='{
              "--lumo-font-size": "1rem",
              "--lumo-font-size-xxxl": "3rem",
              "--lumo-font-size-xxl": "2.25rem",
              "--lumo-font-size-xl": "1.75rem",
              "--lumo-font-size-l": "1.375rem",
              "--lumo-font-size-m": "1.125rem",
              "--lumo-font-size-s": "1rem",
              "--lumo-font-size-xs": "0.875rem",
              "--lumo-font-size-xxs": "0.8125rem"
            }'
              >
                L
              </preset-item>
            </vaadin-list-box>
          </template>
        </preset-picker>

        <preset-picker label="Line height" class="line-height">
          <template>
            <vaadin-list-box>
              <preset-item
                value="s"
                properties='{
                "--lumo-line-height-m": "1.4",
                "--lumo-line-height-s": "1.2",
                "--lumo-line-height-xs": "1.1"
            }'
              >
                Tight
              </preset-item>
              <preset-item
                value=""
                properties='{
                "--lumo-line-height-m": null,
                "--lumo-line-height-s": null,
                "--lumo-line-height-xs": null
            }'
              >
                Normal
              </preset-item>
              <preset-item
                value="loose"
                properties='{
                "--lumo-line-height-m": "1.8",
                "--lumo-line-height-s": "1.5",
                "--lumo-line-height-xs": "1.3"
            }'
              >
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
      { label: 'System default', value: '' },
      { label: 'Helvetica', value: 'Helvetica Neue, Arial, Helvetica, sans-serif' },
      { label: 'Verdana', value: 'Verdana, Geneva, sans-serif' }
    ];

    // document.addEventListener(LumoEditor.PROPERTY_CHANGED, e => {
    //   var entry = e.detail;
    // });
  }
}

customElements.define(Typography.is, Typography);
