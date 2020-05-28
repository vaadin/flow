/* tslint:disable:max-classes-per-file */

import { directive, Part, PropertyPart } from "lit-html";
import { AbstractModel, fromStringSymbol, getName, getValue, requiredSymbol, setValue } from "./Models";
import { validate, ValueError } from "./Validation";

export const fieldSymbol = Symbol('field');

interface Field {
  required: boolean,
  invalid: boolean,
  errorMessage: string
  value: any,
}
interface FieldState extends Field {
  name: string,
  visited: boolean
}
const fieldStateMap = new WeakMap<PropertyPart, FieldState>();

export interface FieldStrategy extends Field {
  element: Element;
  validate: () => Promise<Array<ValueError<any>>>;
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

function getFieldStrategy(elm: any): FieldStrategy {
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
  const fieldStrategy = getFieldStrategy(element);

  if (fieldStateMap.has(propertyPart)) {
    fieldState = fieldStateMap.get(propertyPart)!;
  } else {
    fieldState = {
      name: '',
      value: '',
      required: false,
      invalid: false,
      errorMessage: '',
      visited: false
    };
    fieldStateMap.set(propertyPart, fieldState);
    (model as any)[fieldSymbol] = fieldStrategy;

    fieldStrategy.validate = async () => {
      fieldState.visited = true;
      const errors = await validate(model);

      const displayedError = errors[0];
      fieldStrategy.invalid = fieldState.invalid = displayedError !== undefined;
      fieldStrategy.errorMessage = fieldState.errorMessage = displayedError?.validator?.message || '';

      if (effect !== undefined) {
        effect.call(element, element);
      }
      return errors;
    };

    const updateValueFromElement = () => {
      fieldState.value = fieldStrategy.value;
      const convert = typeof fieldState.value === 'string' && (model as any)[fromStringSymbol];
      setValue(model, convert ? convert(fieldState.value) : fieldState.value);
      if (effect !== undefined) {
        effect.call(element, element);
      }
    };

    element.oninput = () => {
      updateValueFromElement();
      if (fieldState.visited) {
        fieldStrategy.validate();
      }
    };

    element.onchange = element.onblur = () => {
      updateValueFromElement();
      fieldStrategy.validate();
    };

    element.checkValidity = () => !fieldState.invalid;
  }

  const name = getName(model);
  if (name !== fieldState.name) {
    fieldState.name = name;
    element.setAttribute('name', name);
  }

  const value = getValue(model);
  if (value !== fieldState.value) {
    fieldState.value = value;
    fieldStrategy.value = value;
  }

  const required = model[requiredSymbol];
  if (required !== fieldState.required) {
    fieldState.required = required;
    fieldStrategy.required = required;
  }
});
