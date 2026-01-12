import { html, LitElement } from 'lit';

class TodoCreator extends LitElement {
  static get properties() {
    return {
      task: {
        type: String
      },
      user: {
        type: String
      }
    };
  }
  constructor() {
    super();
    this.task = '';
    this.user = '';
  }

  render() {
    return html`
      <div id="todoEntry">
        <label for="task-input">Task</label>
        <input type="text" @change=${this.updateTask} id="task-input" style="width: 25%; display: inline-block" />
        <label for="user-name-input">Username</label>
        <input
          type="text"
          @change=${this.updateName}
          char-counter
          maxlength="16"
          error="maximum characters exceeded"
          id="user-name-input"
          style="width: 25%; display: inline-block"
        />
        <button icon="add" @click="${this.postTask}" id="create-button">+</button>
      </div>
    `;
  }

  updateName(e) {
    this.user = e.srcElement.value;
  }

  updateTask(e) {
    this.task = e.srcElement.value;
  }

  postTask() {
    let tsk = this.task;
    let usr = this.user;

    if (tsk == '') {
      alert('Task is Empty!');
      return;
    }
    if (usr == '') {
      usr = 'Anonymous';
    }
    this.task = '';

    this.$server.createTodo(tsk, usr);
  }
}
customElements.define('todo-creator', TodoCreator);
