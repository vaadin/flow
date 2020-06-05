/* tslint:disable:max-classes-per-file */

import { FieldStrategy, fieldSymbol } from "./Field";
import { AbstractModel, ArrayModel, getName, getValue, requiredSymbol, validatorsSymbol } from "./Models";
import { Required } from "./Validators";

export interface ValueError<T> {
  property: string | AbstractModel<any>,
  value: T,
  validator: Validator<T>
}

export class ValidationError extends Error {
  constructor(public errors:Array<ValueError<any>>) {
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

function validateModel<T>(model: AbstractModel<T>) {
  const fieldStrategy = (model as any)[fieldSymbol] as FieldStrategy;
  return fieldStrategy ? fieldStrategy.validate() : validate(model);
}

async function runValidator<T>(model: AbstractModel<T>, validator: Validator<T>) {
  const value = getValue(model);
  // if model is not required and value empty, do not run any validator
  if (!model[requiredSymbol] && !new Required().validate(value)) {
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

export async function validate<T>(model: AbstractModel<T>): Promise<Array<ValueError<any>>> {
  const promises: Array<Promise<Array<ValueError<any>> | ValueError<any> | void>> = [];
  // validate each model in the array model
  if (model instanceof ArrayModel) {
    promises.push(...[...model].map(validateModel));
  }
  // validate each model property
  const properties = Object.getOwnPropertyNames(model).filter(name => (model as any)[name] instanceof AbstractModel);
  promises.push(...[...properties].map(prop => (model as any)[prop]).map(validateModel));
  // run all model validators
  promises.push(...[...model[validatorsSymbol]].map(validator => runValidator(model, validator)));
  // wait for all promises and return errors
  return((await Promise.all(promises) as any).flat()).filter(Boolean);
}
