import { activateShim, ComponentReference, deactivateShim, getComponent, getComponents, highlight } from './shim';

type EditHandler = (editComponent: ComponentReference, editType: string, value: string) => void;
type ListenerHandler = (editComponent: ComponentReference, listenerType: string) => void;

let activeEditor: HTMLInputElement | undefined;
let activeEditHandler: EditHandler | undefined;
let activeListenerHandler: ListenerHandler | undefined;
let editComponent: ComponentReference | undefined;
let editType: 'setLabel' | 'setText' | undefined;
let restoreElement: (() => void) | undefined;
let newValueAssigner: ((newValue: string) => void) | undefined;

export function activateEditMode(editHandler: EditHandler, listenerHandler: ListenerHandler) {
  activeEditHandler = editHandler;
  activeListenerHandler = listenerHandler;
  activateShim(shimMove, shimClick, shimContextMenu);
}
export function deactiveEditMode() {
  deactivateShim();
  activeEditHandler = undefined;
}

function shimMove(targetElement: HTMLElement, _e: MouseEvent): void {
  if (isLabel(targetElement) || isButton(targetElement)) {
    highlight(targetElement);
  } else {
    highlight(undefined);
  }
}

function shimClick(targetElement: HTMLElement, _e: MouseEvent): void {
  if (isLabel(targetElement)) {
    editLabel(targetElement);
    deactivateShim();
  } else if (isButton(targetElement)) {
    editButton(targetElement);
    deactivateShim();
  }
}

function shimContextMenu(targetElement: HTMLElement, _e: MouseEvent): void {
  if (activeListenerHandler) {
    if (targetElement.tagName === 'VAADIN-BUTTON') {
      activeListenerHandler(getComponent(targetElement), 'addClickListener');
    }
  }
}

function getInputEditor(): HTMLInputElement {
  const labelEditor = document.createElement('input');
  labelEditor.type = 'text';
  labelEditor.addEventListener('blur', () => {
    if (editComponent) {
      // Might have been handled by 'enter' already
      commitEdit(labelEditor.value);
    }
  });

  labelEditor.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      commitEdit(labelEditor.value);
    } else if (e.key === 'Escape') {
      abortEdit();
    }
    e.stopPropagation();
  });

  return labelEditor;
}
function isLabel(targetElement: HTMLElement) {
  return targetElement.tagName.toLowerCase() === 'label' && targetElement.getAttribute('slot') === 'label';
}

function isButton(targetElement: HTMLElement) {
  return targetElement.tagName.toLowerCase() === 'vaadin-button';
}
function editLabel(targetElement: HTMLElement) {
  const editor = getInputEditor();
  editor.value = targetElement.innerText;

  const editorWrapper = document.createElement('label');
  editorWrapper.slot = targetElement.slot;
  editorWrapper.append(editor);

  const storedElement = targetElement;
  editComponent = getComponents(storedElement).pop();
  storedElement.parentElement!.replaceChild(editorWrapper, storedElement);
  restoreElement = () => {
    editorWrapper!.parentElement!.replaceChild(storedElement!, editorWrapper!);
  };
  newValueAssigner = (value) => {
    storedElement.innerText = value;
  };
  editType = 'setLabel';
  activeEditor = editor;
  requestAnimationFrame(() => {
    if (activeEditor) {
      editComponent!.element!.setAttribute('has-label', ''); // Prevent text field from hiding it
      activeEditor.focus();
    }
  });
}
function editButton(targetElement: HTMLElement) {
  const editor = getInputEditor();
  editor.slot = targetElement.slot;
  editor.value = targetElement.innerText;
  const storedText = targetElement.innerText;
  editComponent = getComponents(targetElement).pop();
  targetElement.innerText = '';
  targetElement.appendChild(editor);

  restoreElement = () => {
    targetElement.innerText = storedText;
  };
  newValueAssigner = (value) => {
    targetElement.innerText = value;
  };
  editType = 'setText';
  activeEditor = editor;
  requestAnimationFrame(() => {
    if (activeEditor) {
      activeEditor.focus();
    }
  });
}
function commitEdit(value: string) {
  activeEditHandler!(editComponent!, editType!, value);
  stopEditing();
  newValueAssigner!(value);
}
function abortEdit() {
  stopEditing();
}

function stopEditing() {
  editComponent = undefined;
  editType = undefined;

  activeEditor = undefined;
  if (restoreElement) {
    restoreElement();
    restoreElement = undefined;
  }

  activateShim(shimMove, shimClick, shimContextMenu);
}
