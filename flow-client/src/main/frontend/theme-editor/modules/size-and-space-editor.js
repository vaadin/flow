import { EditorModule } from './editor-module.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
class SizeAndSpace extends EditorModule {
  static get template() {
    return html`
      <style include="shared-editor-module-styles"></style>

      <preset-picker label="Size">
        <template>
          <vaadin-list-box>
            <preset-item
              value="xs"
              properties='{
            "--lumo-size-xl": "2.5rem",
            "--lumo-size-l": "2rem",
            "--lumo-size-m": "1.75rem",
            "--lumo-size-s": "1.5rem",
            "--lumo-size-xs": "1.25rem"
          }'
            >
              Extra small
            </preset-item>
            <preset-item
              value="s"
              properties='{
            "--lumo-size-xl": "3rem",
            "--lumo-size-l": "2.5rem",
            "--lumo-size-m": "2rem",
            "--lumo-size-s": "1.75rem",
            "--lumo-size-xs": "1.5rem"
          }'
            >
              Small
            </preset-item>
            <preset-item
              value=""
              properties='{
            "--lumo-size-xl": null,
            "--lumo-size-l": null,
            "--lumo-size-m": null,
            "--lumo-size-s": null,
            "--lumo-size-xs": null
          }'
            >
              Medium
            </preset-item>
            <preset-item
              value="l"
              properties='{
            "--lumo-size-xl": "4rem",
            "--lumo-size-l": "3rem",
            "--lumo-size-m": "2.5rem",
            "--lumo-size-s": "2rem",
            "--lumo-size-xs": "1.75rem"
          }'
            >
              Large
            </preset-item>
          </vaadin-list-box>
        </template>
      </preset-picker>

      <details>
        <summary>Advanced</summary>
        <table>
          <tbody>
            <tr>
              <th>Extra small</th>
              <td><property-editor name="--lumo-size-xs"></property-editor></td>
            </tr>
            <tr>
              <th>Small</th>
              <td><property-editor name="--lumo-size-s"></property-editor></td>
            </tr>
            <tr>
              <th>Medium</th>
              <td><property-editor name="--lumo-size-m"></property-editor></td>
            </tr>
            <tr>
              <th>Large</th>
              <td><property-editor name="--lumo-size-l"></property-editor></td>
            </tr>
            <tr>
              <th>Extra large</th>
              <td><property-editor name="--lumo-size-xl"></property-editor></td>
            </tr>
          </tbody>
        </table>
      </details>

      <preset-picker label="Space">
        <template>
          <vaadin-list-box>
            <preset-item
              value="xs"
              properties='{
            "--lumo-space-xl": "1.75rem",
            "--lumo-space-l": "1.125rem",
            "--lumo-space-m": "0.5rem",
            "--lumo-space-s": "0.25rem",
            "--lumo-space-xs": "0.125rem"
          }'
            >
              Extra small
            </preset-item>
            <preset-item
              value="s"
              properties='{
            "--lumo-space-xl": "1.875rem",
            "--lumo-space-l": "1.25rem",
            "--lumo-space-m": "0.625rem",
            "--lumo-space-s": "0.3125rem",
            "--lumo-space-xs": "0.1875rem"
          }'
            >
              Small
            </preset-item>
            <preset-item
              value=""
              properties='{
            "--lumo-space-xl": null,
            "--lumo-space-l": null,
            "--lumo-space-m": null,
            "--lumo-space-s": null,
            "--lumo-space-xs": null
          }'
            >
              Medium
            </preset-item>
            <preset-item
              value="l"
              properties='{
            "--lumo-space-xl": "2.5rem",
            "--lumo-space-l": "1.75rem",
            "--lumo-space-m": "1.125rem",
            "--lumo-space-s": "0.75rem",
            "--lumo-space-xs": "0.375rem"
          }'
            >
              Large
            </preset-item>
          </vaadin-list-box>
        </template>
      </preset-picker>

      <details>
        <summary>Advanced</summary>
        <table>
          <tbody>
            <tr>
              <th>Extra small</th>
              <td><property-editor name="--lumo-space-xs"></property-editor></td>
            </tr>
            <tr>
              <th>Small</th>
              <td><property-editor name="--lumo-space-s"></property-editor></td>
            </tr>
            <tr>
              <th>Medium</th>
              <td><property-editor name="--lumo-space-m"></property-editor></td>
            </tr>
            <tr>
              <th>Large</th>
              <td><property-editor name="--lumo-space-l"></property-editor></td>
            </tr>
            <tr>
              <th>Extra large</th>
              <td><property-editor name="--lumo-space-xl"></property-editor></td>
            </tr>
          </tbody>
        </table>
      </details>
    `;
  }

  static get is() {
    return 'size-and-space-editor';
  }
}

customElements.define(SizeAndSpace.is, SizeAndSpace);
