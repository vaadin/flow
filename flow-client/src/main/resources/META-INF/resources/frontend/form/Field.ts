/* tslint:disable:max-classes-per-file */

import { directive, Part, PropertyPart } from "lit-html";
import {
  _fromString,
  AbstractModel,
  getBinderNode
} from "./Models";

interface Field {
  required: boolean,
  invalid: boolean,
  errorMessage: string
  value: any,
}
interface FieldState extends Field {
  name: string,
  strategy: FieldStrategy
}
const fieldStateMap = new WeakMap<PropertyPart, FieldState>();

export interface FieldStrategy extends Field {
  element: Element;
}

export abstract class AbstractFieldStrategy implements FieldStrategy {
  abstract required: boolean;
  abstract invalid: boolean;
  constructor(public element: Element & Field) {}
  validate = async () => [];
  get value() {return this.element.value}
  set value(value) {this.element.value = value}
  set errorMessage(_: string) { }
  setAttribute(key: string, val: any) {
    if (val) {
      this.element.setAttribute(key, '');
    } else {
      this.element.removeAttribute(key);
    }
  }
}

export class VaadinFieldStrategy extends AbstractFieldStrategy {
  set required(value: boolean) { this.element.required = value }
  set invalid(value: boolean) { this.element.invalid = value }
  set errorMessage(value: string) { this.element.errorMessage = value }
}

export class GenericFieldStrategy extends AbstractFieldStrategy {
  set required(value: boolean) { this.setAttribute('required', value) }
  set invalid(value: boolean) { this.setAttribute('invalid', value) }
}

export class CheckedFieldStrategy extends GenericFieldStrategy {
  set value(val: any) {
    (this.element as any).checked = /^(true|on)$/i.test(String(val));
  }
  get value() {
    return (this.element as any).checked;
  }
}

export class SelectedFieldStrategy extends GenericFieldStrategy {
  set value(val: any) {
    (this.element as any).selected = val;
  }
  get value() {
    return (this.element as any).selected;
  }
}

export function getDefaultFieldStrategy(elm: any): FieldStrategy {
  switch(elm.localName) {
    case 'vaadin-checkbox': case 'vaadin-radio-button':
      return new CheckedFieldStrategy(elm);
    case 'vaadin-list-box':
      return new SelectedFieldStrategy(elm);
    case 'vaadin-rich-text-editor':
      return new GenericFieldStrategy(elm);
    case 'input': if (/^(checkbox|radio)$/.test(elm.type)) {
      return new CheckedFieldStrategy(elm);
    }
  }
  return elm.constructor.version ? new VaadinFieldStrategy(elm) : new GenericFieldStrategy(elm);
}

/**
 * Binds a form field component into a model.
 * 
 * Exmaple usage:
 * 
 * ```
 * <vaadin-text-field ...="${field(model.name)}">
 * </vaadin-text-field>
 * ```
 */
export const field = directive(<T>(
  model: AbstractModel<T>,
  effect?: (element: Element) => void
) => (part: Part) => {
  const propertyPart = part as PropertyPart;
  if (!(part instanceof PropertyPart) || propertyPart.committer.name !== '..') {
    throw new Error('Only supports ...="" syntax');
  }
  let fieldState: FieldState;
  const element = propertyPart.committer.element as HTMLInputElement & Field;

  const binderNode = getBinderNode(model);
  const fieldStrategy = binderNode.binder.getFieldStrategy(element);

  if (fieldStateMap.has(propertyPart)) {
    fieldState = fieldStateMap.get(propertyPart)!;
  } else {
    fieldState = {
      name: '',
      value: '',
      required: false,
      invalid: false,
      errorMessage: '',
      strategy: fieldStrategy
    };
    fieldStateMap.set(propertyPart, fieldState);

    const updateValueFromElement = () => {
      fieldState.value = fieldState.strategy.value;
      const convert = typeof fieldState.value === 'string' && (model as any)[_fromString];
      binderNode.value = convert ? convert(fieldState.value) : fieldState.value;
      if (effect !== undefined) {
        effect.call(element, element);
      }
    };

    element.oninput = () => {
      updateValueFromElement();
    };

    element.onchange = element.onblur = () => {
      updateValueFromElement();
      binderNode.visited = true;
    };

    element.checkValidity = () => !fieldState.invalid;
  }

  const name = binderNode.name;
  if (name !== fieldState.name) {
    fieldState.name = name;
    element.setAttribute('name', name);
  }

  const value = binderNode.value;
  if (value !== fieldState.value) {
    fieldState.strategy.value = fieldState.value = value;
  }

  const required = binderNode.required;
  if (required !== fieldState.required) {
    fieldState.strategy.required = fieldState.required = required;
  }

  const firstError = binderNode.ownErrors ? binderNode.ownErrors[0] : undefined;
  const errorMessage = firstError && firstError.message || '';
  if (errorMessage !== fieldState.errorMessage) {
    fieldState.strategy.errorMessage = fieldState.errorMessage = errorMessage;
  }

  const invalid = binderNode.invalid;
  if (invalid !== fieldState.invalid) {
    fieldState.strategy.invalid = fieldState.invalid = invalid;
  }
});
