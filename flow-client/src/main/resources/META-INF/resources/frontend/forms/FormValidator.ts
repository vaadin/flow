/* tslint:disable:max-classes-per-file */

import * as validator from 'validator';
import { FieldElement } from './Field';
import { AbstractModel, ArrayModel, fieldSymbol, getName, getValue, requiredSymbol, validatorsSymbol } from './Models';

interface FormOptions {message?: string};
type ValueNumberOptions = FormOptions & {value: number | string};
type DigitOptions = FormOptions & {integer: number, fraction: number};
type SizeOptions = FormOptions & {min?: number, max?: number};
type PatternOptions = FormOptions & {regexp: RegExp | string};
type DecimalOptions = ValueNumberOptions & {inclusive: boolean | undefined};

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



abstract class AbstractValidator<T> implements Validator<T> {
  message = 'invalid';
  abstract validate: ValidationCallback<T>;
  constructor(options?: FormOptions) {
    if (options && options.message) {
      this.message = options.message;
    }
  }
}
abstract class ValueNumberValidator<T> extends AbstractValidator<T> {
  value: number;
  constructor(opts: ValueNumberOptions | number | string) {
    super(typeof opts === 'number' || typeof opts === 'string' ? {} : opts);
    const val = typeof opts === 'object' ? opts.value : opts;
    this.value = typeof val === 'string' ? parseFloat(val) : val;
  }
}

// JSR380 equivalent (https://beanvalidation.org/2.0/spec/#builtinconstraints)
export class Email extends AbstractValidator<string> {
  validate = (value: string) => validator.isEmail(value);
}
export class Null extends AbstractValidator<any> {
  validate = (value: any) => value == null;
}
export class NotNull extends AbstractValidator<any> {
  validate = (value: any) => !new Null().validate(value);
}
export class NotEmpty extends AbstractValidator<any> {
  validate = (value: any) => new NotNull().validate(value) && !validator.isEmpty(value);
}
export class NotBlank extends AbstractValidator<any> {
  validate = (value: any) => new NotEmpty().validate(value);
}
export class AssertTrue extends AbstractValidator<any> {
  validate = (value: any) => validator.isBoolean(String(value)) && String(value) === 'true';
}
export class AssertFalse extends AbstractValidator<any> {
  validate = (value: any) => !new AssertTrue().validate(value);
}
export class Min extends ValueNumberValidator<any> {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {min: this.value})
}
export class Max extends ValueNumberValidator<any> {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {max: this.value})
}
export class DecimalMin extends ValueNumberValidator<any> {
  inclusive: boolean;
  constructor(opts: DecimalOptions | string | number) {
    super(opts);
    this.inclusive = typeof opts !== 'object' || opts.inclusive !== false;
  }
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {[this.inclusive ? 'min' : 'gt']: this.value});
}
export class DecimalMax extends DecimalMin {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {[this.inclusive ? 'max' : 'lt']: this.value})
}
export class Negative extends AbstractValidator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) < 0;
}
export class NegativeOrZero extends AbstractValidator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) <= 0;
}
export class Positive extends AbstractValidator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) > 0;
}
export class PositiveOrZero extends AbstractValidator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) >= 0;
}

export class Size extends AbstractValidator<string> {
  value: SizeOptions;
  constructor(opts: SizeOptions) {
    super(opts);
    this.value = {min: opts.min || 0, max: opts.max || 0x7fffffff};
  }
  validate = (value: string) => validator.isLength(value, this.value);
}

export class Digits extends AbstractValidator<string> {
  value: DigitOptions;
  constructor(opts: FormOptions & DigitOptions) {
    super(opts);
    this.value = {integer: opts.integer, fraction: opts.fraction};
  }
  validate = (value: any) =>
      String(validator.toFloat(`${value}`)).replace(/(.*)\.\d+/, "$1").length === this.value.integer
      && validator.isDecimal(`${value}`, {decimal_digits: `0,${this.value.fraction}`})
}

export class Past extends AbstractValidator<any> {
  validate = (value: any) => validator.isBefore(value);
}
export class PastOrPresent extends AbstractValidator<any> {
  validate = () => {throw new Error('Form Validator for PastOrPresent not implemented yet')};
}
export class Future extends AbstractValidator<any> {
  validate = (value: any) => validator.isAfter(value);
}
export class FutureOrPresent extends AbstractValidator<any> {
  validate = () => {throw new Error('Form Validator for FutureOrPresent not implemented yet')};
}

export class Pattern extends AbstractValidator<string> {
  value: RegExp;
  constructor(opts: PatternOptions | string | RegExp) {
    super(typeof opts === 'string' || opts instanceof RegExp ? {} : opts);
    this.value = typeof opts === 'string' ? new RegExp(opts)
      : opts instanceof RegExp ? opts
      : typeof opts.regexp === 'string' ? new RegExp(opts.regexp) : opts.regexp;
  }
  validate = (value: any) => validator.matches(value, this.value);
}

function validateModel<T>(model: AbstractModel<T>) {
  const fieldElement = (model as any)[fieldSymbol] as FieldElement;
  return fieldElement ? fieldElement.validate() : validate(model);
}

async function runValidator<T>(model: AbstractModel<T>, myValidator: Validator<T>) {
  const value = getValue(model);
  // if model is not required and value empty, do not run any validator
  if (!model[requiredSymbol] && !new Required().validate(value)) {
    return undefined;
  }
  return (async() => myValidator.validate(value))()
    .then(valid => valid ? undefined 
      : {property: getName(model), value, myValidator});
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
  promises.push(...[...model[validatorsSymbol]].map(formValidator => runValidator(model, formValidator)));
  // wait for all promises and return errors
  return((await Promise.all(promises) as any).flat()).filter(Boolean);
}