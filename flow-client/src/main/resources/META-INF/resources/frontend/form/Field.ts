/* tslint:disable:max-classes-per-file */

import {
  AttributePartInfo,
  directive,
  Directive,
  ElementPartInfo,
  PartInfo,
  PartType,
} from "lit/directive";
import { BinderNode } from "./BinderNode";
import { _fromString, AbstractModel, getBinderNode } from "./Models";

interface Field {
  required: boolean;
  invalid: boolean;
  errorMessage: string;
  value: any;
}
interface FieldState extends Field {
  name: string;
  strategy: FieldStrategy;
}
export interface FieldStrategy extends Field {
  element: Element;
}

export abstract class AbstractFieldStrategy implements FieldStrategy {
  abstract required: boolean;
  abstract invalid: boolean;
  constructor(public element: Element & Field) {}
  validate = async () => [];
  get value() {
    return this.element.value;
  }
  set value(value) {
    this.element.value = value;
  }
  set errorMessage(_: string) {}
  setAttribute(key: string, val: any) {
    if (val) {
      this.element.setAttribute(key, "");
    } else {
      this.element.removeAttribute(key);
    }
  }
}

export class VaadinFieldStrategy extends AbstractFieldStrategy {
  set required(value: boolean) {
    this.element.required = value;
  }
  set invalid(value: boolean) {
    this.element.invalid = value;
  }
  set errorMessage(value: string) {
    this.element.errorMessage = value;
  }
}

export class GenericFieldStrategy extends AbstractFieldStrategy {
  set required(value: boolean) {
    this.setAttribute("required", value);
  }
  set invalid(value: boolean) {
    this.setAttribute("invalid", value);
  }
}

export class CheckedFieldStrategy extends GenericFieldStrategy {
  set value(val: any) {
    (this.element as any).checked = /^(true|on)$/i.test(String(val));
  }
  get value() {
    return (this.element as any).checked;
  }
}

export class ComboBoxFieldStrategy extends VaadinFieldStrategy {
  get value() {
    const selectedItem = (this.element as any).selectedItem;
    return selectedItem === null ? undefined : selectedItem;
  }
  set value(val: any) {
    (this.element as any).selectedItem = val === undefined ? null : val;
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
  switch (elm.localName) {
    case "vaadin-checkbox":
    case "vaadin-radio-button":
      return new CheckedFieldStrategy(elm);
    case "vaadin-combo-box":
      return new ComboBoxFieldStrategy(elm);
    case "vaadin-list-box":
      return new SelectedFieldStrategy(elm);
    case "vaadin-rich-text-editor":
      return new GenericFieldStrategy(elm);
    case "input":
      if (/^(checkbox|radio)$/.test(elm.type)) {
        return new CheckedFieldStrategy(elm);
      }
  }
  return elm.constructor.version
    ? new VaadinFieldStrategy(elm)
    : new GenericFieldStrategy(elm);
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
export const field = directive(
  class extends Directive {
    fieldState: FieldState = {
      name: "",
      value: "",
      required: false,
      invalid: false,
      errorMessage: "",
      // @ts-ignore
      strategy: undefined,
    };
    partInfo: AttributePartInfo | ElementPartInfo;
    model!: AbstractModel<any>;
    element!: HTMLInputElement & Field;
    elementInited = false;
    effect: ((element: Element) => void) | undefined;

    constructor(partInfo: PartInfo) {
      super(partInfo);
      if (partInfo.type !== PartType.PROPERTY && partInfo.type !== PartType.ELEMENT) {
        throw new Error('Use as "<element \${field(...)}" or <element ...=\${field(...)}"');
      }
      this.partInfo = partInfo;
    }
    convertFieldValue(fieldValue: any) {
      const fromString = (this.model as any)[_fromString];
      return typeof fieldValue === "string" && fromString
        ? fromString(fieldValue)
        : fieldValue;
    }
    updateValueFromElement = (binderNode: BinderNode<any, any>) => {
      this.fieldState.value = this.fieldState.strategy.value;
      binderNode.value = this.convertFieldValue(this.fieldState.value);
      if (this.effect !== undefined) {
        this.effect.call(this.element, this.element);
      }
    };

    render(model: AbstractModel<any>, effect?: (element: Element) => void) {
      this.element = (this.partInfo as any).element as HTMLInputElement & Field;
      this.model = model;
      this.effect = effect;
      const binderNode = getBinderNode(model);
      this.fieldState.strategy = binderNode.binder.getFieldStrategy(
        this.element
      );
      if (!this.elementInited) {
        this.elementInited = true;

        this.element.oninput = () => {
          this.updateValueFromElement(binderNode);
        };

        this.element.onchange = this.element.onblur = () => {
          this.updateValueFromElement(binderNode);
          binderNode.visited = true;
        };

        this.element.checkValidity = () => !this.fieldState.invalid;
      }

      const name = binderNode.name;
      if (name !== this.fieldState.name) {
        this.fieldState.name = name;
        this.element.setAttribute("name", name);
      }

      const value = binderNode.value;
      const valueFromField = this.convertFieldValue(this.fieldState.value);
      if (
        value !== valueFromField &&
        !(Number.isNaN(value) && Number.isNaN(valueFromField))
      ) {
        this.fieldState.strategy.value = this.fieldState.value = value;
      }

      const required = binderNode.required;
      if (required !== this.fieldState.required) {
        this.fieldState.strategy.required = this.fieldState.required = required;
      }

      const firstError = binderNode.ownErrors
        ? binderNode.ownErrors[0]
        : undefined;
      const errorMessage = (firstError && firstError.message) || "";
      if (errorMessage !== this.fieldState.errorMessage) {
        this.fieldState.strategy.errorMessage = this.fieldState.errorMessage = errorMessage;
      }

      const invalid = binderNode.invalid;
      if (invalid !== this.fieldState.invalid) {
        this.fieldState.strategy.invalid = this.fieldState.invalid = invalid;
      }
    }
  }
);
