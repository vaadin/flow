/* tslint:disable:max-classes-per-file */

import { Binder } from "./Binder";
import {
  AbstractModel,
  getBinderNode,
} from "./Models";
import { Required } from "./Validators";

export interface ValueError<T> {
  property: string | AbstractModel<any>,
  message: string,
  value: T,
  validator: Validator<T>
}

export interface ValidationResult {
  property: string | AbstractModel<any>,
  message?: string
}

export class ValidationError extends Error {
  constructor(public errors:ReadonlyArray<ValueError<any>>) {
    super([
      "There are validation errors in the form.",
      ...errors.map(e => `${e.property} - ${e.validator.constructor.name}${e.message? ': ' + e.message : ''}`)
    ].join('\n - '));
    this.name = this.constructor.name;
  }
}

export type ValidationCallback<T> = (value: T, binder: Binder<any, AbstractModel<T>>) => boolean | ValidationResult | ReadonlyArray<ValidationResult> | Promise<boolean | ValidationResult | ReadonlyArray<ValidationResult>>;

export interface Validator<T> {
  validate: ValidationCallback<T>,
  message: string,
  impliesRequired?: boolean
}

export class ServerValidator implements Validator<any> {
  constructor(public message: string) {
  }
  validate = () => false;
}

export async function runValidator<T>(model: AbstractModel<T>, validator: Validator<T>): Promise<ReadonlyArray<ValueError<T>>> {
  const value = getBinderNode(model).value;
  // if model is not required and value empty, do not run any validator
  if (!getBinderNode(model).required && !new Required().validate(value)) {
    return [];
  }
  return (async () => validator.validate(value, getBinderNode(model).binder))()
    .then(result => {
      if (result === false) {
        return [{ property: getBinderNode(model).name, value, validator, message: validator.message }];
      } else if (result === true || Array.isArray(result) && result.length === 0) {
        return [];
      } else if (Array.isArray(result)) {
        return result.map(result2 => ({ message: validator.message, ...result2, value, validator }));
      } else {
        return [{ message: validator.message, ...result, value, validator }];
      }
    });
}
