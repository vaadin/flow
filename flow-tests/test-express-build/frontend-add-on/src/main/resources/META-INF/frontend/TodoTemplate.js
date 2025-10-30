import { css, html, LitElement } from 'lit';

class TodoTemplate extends LitElement {
  static get styles() {
    return css`
      .main-panel {
        background-color: #eee;
        height: 100vh;
        overflow-y: hidden;
        padding: 20px;
        flex: 1;
      }

      .side-panel {
        padding: 20px 10px;
        width: 30%;
        border-right: 1px solid black;
      }

      .content-wrap {
        width: 100%;
        display: flex;
      }
    `;
  }

  render() {
    return html`
      <todo-creator id="creator"></todo-creator>

      <div class="content-wrap">
        <div class="side-panel">
          <h2>Completed tasks</h2>
          <slot name="done"></slot>
        </div>

        <div class="main-panel">
          <h2>Open tasks</h2>
          <slot>
            <h3>No tasks are waiting</h3>
          </slot>
        </div>
      </div>
    `;
  }
}

customElements.define('todo-template', TodoTemplate);
