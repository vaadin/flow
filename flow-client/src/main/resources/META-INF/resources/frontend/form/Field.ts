/* tslint:disable:max-classes-per-file */

import { directive, Part, PropertyPart } from "lit-html";
import {
  AbstractModel,
  fromStringSymbol,
  getBinderNode
} from "./Models";

interface Field {
  required: boolean,
  invalid: boolean,
  errorMessage: string
}

export interface FieldStrategy extends Field {
  element: Element;
}

interface FieldState extends Field {
  name: string,
  value: string,
  strategy: FieldStrategy
}
const fieldStateMap = new WeakMap<PropertyPart, FieldState>();

class VaadinFieldStrategy implements FieldStrategy {
  constructor(public element: Element & Field) {}
  set required(value: boolean) { this.element.required = value }
  set invalid(value: boolean) { this.element.invalid = value }
  set errorMessage(value: string) { this.element.errorMessage = value }
}

class GenericFieldStrategy implements FieldStrategy {
  constructor(public element: Element) {}
  set required(value: boolean) { this.setAttribute('required', value) }
  set invalid(value: boolean) { this.setAttribute('invalid', value) }
  set errorMessage(_: string) { }
  setAttribute(key: string, val: any) {
    if (val) {
      this.element.setAttribute(key, '');
    } else {
      this.element.removeAttribute(key);
    }
  }
}

// vaadin elements have a `version` static property in the class
const isVaadinElement = (elm: Element) => (elm.constructor as any).version;

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

  if (!fieldStateMap.has(propertyPart)) {
    fieldState = {
      name: '',
      value: '',
      required: false,
      invalid: false,
      errorMessage: '',
      strategy: isVaadinElement(element) ? new VaadinFieldStrategy(element) : new GenericFieldStrategy(element)
    };

    const updateValueFromElement = () => {
      fieldState.value = element.value;
      binderNode.value = (model as any)[fromStringSymbol](element.value) as T;
      if (effect !== undefined) {
        effect.call(element, element);
      }
    };

    element.oninput = () => {
      updateValueFromElement();
      if (binderNode.visited) {
        binderNode.validate();
      }
    };

    element.onchange = element.onblur = () => {
      updateValueFromElement();
      binderNode.visited = true;
      binderNode.validate();
    };

    element.checkValidity = () => !fieldState.invalid;
  } else {
    fieldState = fieldStateMap.get(propertyPart)!;
  }

  const name = binderNode.name;
  if (name !== fieldState.name) {
    fieldState.name = name;
    element.setAttribute('name', name);
  }

  const value = String(binderNode.value);
  if (value !== fieldState.value) {
    fieldState.value = value;
    element.value = value;
    // fieldState.strategy.value = value;
  }

  const firstError = binderNode.ownErrors[0];
  const errorMessage = firstError && firstError.validator.message || '';
  if (errorMessage !== fieldState.errorMessage) {
    fieldState.errorMessage = errorMessage;
    fieldState.strategy.errorMessage = errorMessage;
  }

  const required = binderNode.required;
  if (required !== fieldState.required) {
    fieldState.required = required;
    fieldState.strategy.required = required;
  }
});
