import { html, LitElement } from 'lit';

class TodoElement extends LitElement {
  static get properties() {
    return {
      editing: { type: Boolean },
      completed: { type: Boolean },
      remove: { type: Boolean },
      task: { type: String },
      user: { type: String },
      rid: { type: String },
      time: { type: String }
    };
  }
  constructor() {
    super();
    this.editing = false;
    this.completed = false;
    this.task = '';
    this.user = '';
    this.rid = '';
    this.time = '';
  }

  render() {
    return html`
      <div class="todo" elevation="1">
        <input type="checkbox" ?hidden=${this.completed} ?checked=${this.complete} id="checkbox" />

        ${this.completed
          ? html`<button icon="icons:delete" @click="${this.delete}">REMOVE</button>`
          : html`<button ?hidden=${this.editing} icon="icons:create" @click="${this._doEdit}" class="edit">EDIT</button>
              <button ?hidden=${!this.editing} icon="icons:done" @click="${this._doEdit}" class="done">SAVE</button>`}
        ${this.editing
          ? html`<input type="text" id="edit"  @change="${this.updateTask}"></input>`
          : html`<span id="task">${this.task}</span>`}

        <div class="info">Created by: <span>${this.user}</span></div>
        <div class="info">${this.time}</div>
      </div>
    `;
  }

  complete() {
    this.completed = true;
  }

  delete() {
    this.remove = true;
  }

  updateTask(e) {
    this.task = e.srcElement.value;
  }

  _doEdit() {
    if (this.editing) {
      this.task = document.getElementById('edit').value;
    }
    this.editing = !this.editing;
  }
}
customElements.define('todo-element', TodoElement);
