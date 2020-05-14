/* tslint:disable:max-classes-per-file */

import { FieldElement, fieldSymbol } from "./Field";
import { AbstractModel, ArrayModel, getName, getValue, requiredSymbol, validatorsSymbol } from "./Models";

export interface ValueError<T> {
  property: string,
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

export type ValidationCallback<T> = (value: T) => boolean | Promise<boolean>;

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

export function getModelValidators<T>(model: AbstractModel<T>): Set<Validator<T>> {
  return model[validatorsSymbol];
}

function validateModel<T>(model: AbstractModel<T>) {
  const fieldElement = (model as any)[fieldSymbol] as FieldElement;
  return fieldElement ? fieldElement.validate() : validate(model);
}

async function runValidator<T>(model: AbstractModel<T>, validator: Validator<T>) {
  const value = getValue(model);
  // if model is not required and value empty, do not run any validator
  if (!model[requiredSymbol] && !new Required().validate(value)) {
    return undefined;
  }
  return (async() => validator.validate(value))()
    .then(valid => valid ? undefined
      : {property: getName(model), value, validator});
}

export async function validate<T>(model: AbstractModel<T>): Promise<Array<ValueError<any>>> {
  const promises: Array<Promise<Array<ValueError<any>> | ValueError<any> | undefined>> = [];
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