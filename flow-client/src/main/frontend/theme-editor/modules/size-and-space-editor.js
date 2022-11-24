import { EditorModule } from './editor-module.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
class SizeAndSpace extends EditorModule {
  static get template() {
    return html`
    <style include="shared-editor-module-styles">
    </style>

    <preset-picker label="Size">
      <template>
        <vaadin-list-box>
          <preset-item value="xs" properties="{
            &quot;--lumo-size-xl&quot;: &quot;2.5rem&quot;,
            &quot;--lumo-size-l&quot;: &quot;2rem&quot;,
            &quot;--lumo-size-m&quot;: &quot;1.75rem&quot;,
            &quot;--lumo-size-s&quot;: &quot;1.5rem&quot;,
            &quot;--lumo-size-xs&quot;: &quot;1.25rem&quot;
          }">
            Extra small
          </preset-item>
          <preset-item value="s" properties="{
            &quot;--lumo-size-xl&quot;: &quot;3rem&quot;,
            &quot;--lumo-size-l&quot;: &quot;2.5rem&quot;,
            &quot;--lumo-size-m&quot;: &quot;2rem&quot;,
            &quot;--lumo-size-s&quot;: &quot;1.75rem&quot;,
            &quot;--lumo-size-xs&quot;: &quot;1.5rem&quot;
          }">
            Small
          </preset-item>
          <preset-item value="" properties="{
            &quot;--lumo-size-xl&quot;: null,
            &quot;--lumo-size-l&quot;: null,
            &quot;--lumo-size-m&quot;: null,
            &quot;--lumo-size-s&quot;: null,
            &quot;--lumo-size-xs&quot;: null
          }">
            Medium
          </preset-item>
          <preset-item value="l" properties="{
            &quot;--lumo-size-xl&quot;: &quot;4rem&quot;,
            &quot;--lumo-size-l&quot;: &quot;3rem&quot;,
            &quot;--lumo-size-m&quot;: &quot;2.5rem&quot;,
            &quot;--lumo-size-s&quot;: &quot;2rem&quot;,
            &quot;--lumo-size-xs&quot;: &quot;1.75rem&quot;
          }">
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
          <preset-item value="xs" properties="{
            &quot;--lumo-space-xl&quot;: &quot;1.75rem&quot;,
            &quot;--lumo-space-l&quot;: &quot;1.125rem&quot;,
            &quot;--lumo-space-m&quot;: &quot;0.5rem&quot;,
            &quot;--lumo-space-s&quot;: &quot;0.25rem&quot;,
            &quot;--lumo-space-xs&quot;: &quot;0.125rem&quot;
          }">
            Extra small
          </preset-item>
          <preset-item value="s" properties="{
            &quot;--lumo-space-xl&quot;: &quot;1.875rem&quot;,
            &quot;--lumo-space-l&quot;: &quot;1.25rem&quot;,
            &quot;--lumo-space-m&quot;: &quot;0.625rem&quot;,
            &quot;--lumo-space-s&quot;: &quot;0.3125rem&quot;,
            &quot;--lumo-space-xs&quot;: &quot;0.1875rem&quot;
          }">
            Small
          </preset-item>
          <preset-item value="" properties="{
            &quot;--lumo-space-xl&quot;: null,
            &quot;--lumo-space-l&quot;: null,
            &quot;--lumo-space-m&quot;: null,
            &quot;--lumo-space-s&quot;: null,
            &quot;--lumo-space-xs&quot;: null
          }">
            Medium
          </preset-item>
          <preset-item value="l" properties="{
            &quot;--lumo-space-xl&quot;: &quot;2.5rem&quot;,
            &quot;--lumo-space-l&quot;: &quot;1.75rem&quot;,
            &quot;--lumo-space-m&quot;: &quot;1.125rem&quot;,
            &quot;--lumo-space-s&quot;: &quot;0.75rem&quot;,
            &quot;--lumo-space-xs&quot;: &quot;0.375rem&quot;
          }">
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
