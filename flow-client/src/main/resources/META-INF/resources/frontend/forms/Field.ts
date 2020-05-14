/* tslint:disable:max-classes-per-file */

import { directive, Part, PropertyPart } from "lit-html";
import { validate, ValueError } from "./FormValidator";
import { AbstractModel, fieldSymbol, fromStringSymbol, getName, getValue, requiredSymbol, setValue } from "./Models";


interface Field {
  required: boolean,
  invalid: boolean,
  errorMessage: string
}
interface FieldState extends Field {
  name: string,
  value: string,
  visited: boolean
}
const fieldStateMap = new WeakMap<PropertyPart, FieldState>();

export interface FieldElement extends Field {
  element: Element;
  validate: () => Promise<Array<ValueError<any>>>;
}

class VaadinFieldElement implements FieldElement {
  constructor(public element: Element & Field) {}
  validate = async () => [];
  set required(value: boolean) { this.element.required = value }
  set invalid(value: boolean) { this.element.invalid = value }
  set errorMessage(value: string) { this.element.errorMessage = value }
}

class GenericFieldElement implements FieldElement {
  constructor(public element: Element) {}
  validate = async () => [];
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

  if (!fieldStateMap.has(propertyPart)) {
    fieldState = { name: '', value: '', required: false, invalid: false, errorMessage: '', visited: false};
    fieldStateMap.set(propertyPart, fieldState);
    const fieldElement:FieldElement = (model as any)[fieldSymbol] =
      isVaadinElement(element) ? new VaadinFieldElement(element) : new GenericFieldElement(element);

    fieldElement.validate = async () => {
      fieldState.value = element.value;
      fieldState.visited = true;
      setValue(model, (model as any)[fromStringSymbol](element.value));

      const errors = await validate(model);

      const displayedError = errors[0];
      fieldElement.invalid = fieldState.invalid = displayedError !== undefined;
      fieldElement.errorMessage = fieldState.errorMessage = displayedError?.validator.message || '';

      if (effect !== undefined) {
        effect.call(element, element);
      }
      return errors;
    };

    element.oninput = () => fieldState.visited && fieldElement.validate();
    element.onchange = element.onblur= fieldElement.validate;

    element.checkValidity = () => !fieldState.invalid;
  } else {
    fieldState = fieldStateMap.get(propertyPart)!;
  }

  const name = getName(model);
  if (name !== fieldState.name) {
    fieldState.name = name;
    element.setAttribute('name', name);
  }

  const value = String(getValue(model));
  if (value !== fieldState.value) {
    element.value = value;
  }

  const required = model[requiredSymbol];
  if (required !== fieldState.required) {
    fieldState.required = required;
    element.required = required;
  }
});