/* tslint:disable:max-classes-per-file */

import {
  AbstractModel,
  getBinderNode,
  getName,
  getValue,
} from "./Models";

export interface ValueError<T> {
  property: string | AbstractModel<any>,
  value: T,
  validator: Validator<T>
}

export class ValidationError extends Error {
  constructor(public errors:ReadonlyArray<ValueError<any>>) {
    super([
      "There are validation errors in the form.",
      ...errors.map(e => `${e.property} - ${e.validator.constructor.name}${e.validator.message? ': ' + e.validator.message : ''}`)
    ].join('\n - '));
    this.name = this.constructor.name;
  }
}

export type ValidationCallback<T> = (value: T) => boolean | ValueError<T> | void | Promise<boolean | ValueError<T> | void>;

export interface Validator<T> {
  validate: ValidationCallback<T>,
  message: string,
  value?: any
}

export class ServerValidator implements Validator<any> {
  constructor(public message: string) {
  }
  validate = () => false;
}

export class Required implements Validator<string> {
  message = '';
  validate = (value: any) => {
    if (typeof value === 'string' || Array.isArray(value)) {
      return value.length > 0;
    } else if (typeof value === 'number') {
      return Number.isFinite(value);
    }
    return value !== undefined;
  }
}

export async function runValidator<T>(model: AbstractModel<T>, validator: Validator<T>) {
  const value = getValue(model);
  // if model is not required and value empty, do not run any validator
  if (!getBinderNode(model).required && !new Required().validate(value)) {
    return;
  }
  return (async () => validator.validate(value))()
    .then(result => {
      if (typeof result === "boolean") {
        return result ? undefined
          : { property: getName(model), value, validator }
      } else {
        return result;
      }
    });
}
