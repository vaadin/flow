/* tslint:disable:max-classes-per-file */

import * as validator from 'validator';
import {Validator} from './Binder';

// JSR380 equivalent (https://beanvalidation.org/2.0/spec/#builtinconstraints)
export class Email implements Validator<string> {
  message = 'invalid';
  validate = (value: string) => validator.isEmail(value);
}
export class Null implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => value == null;
}
export class NotNull implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => !new Null().validate(value);
}
export class NotEmpty implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => new NotNull().validate(value) && !validator.isEmpty(value);
}
export class NotBlank implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => new NotEmpty().validate(value);
}
export class AssertTrue implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.isBoolean(String(value)) && String(value) === 'true';
}
export class AssertFalse implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => !new AssertTrue().validate(value);
}
export class Min implements Validator<any> {
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {min: this.value})
}
export class Max implements Validator<any> {
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {max: this.value})
}

export class DecimalMin implements Validator<any> {
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
  validate = (value: any) => validator.isDecimal(`${value}`, {decimal_digits: `${this.value},`, force_decimal: true});
}
export class DecimalMax implements Validator<any> {
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
  validate = (value: any) => validator.isDecimal(`${value}`, {decimal_digits: `0,${this.value}`});
}

export class Negative implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.toFloat(`${value}`) < 0;
}
export class NegativeOrCero implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.toFloat(`${value}`) <= 0;
}

export class Positive implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.toFloat(`${value}`) > 0;
}

export class PositiveOrCero implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.toFloat(`${value}`) >= 0;
}

export class Size implements Validator<string> {
  message = 'invalid';
  value: any;
  constructor(min: number, max: number) {
    this.value = {min, max};
  }
  validate = (value: string) => validator.isLength(value, this.value);
}

export class Digits implements Validator<string> {
  message = 'invalid';
  value: any;
  constructor(integer: number, decimal: number) {
    this.value = {integer, decimal};
  }
  validate = (value: any) =>
      String(validator.toFloat(`${value}`)).replace(/(.*)\.\d+/, "$1").length === this.value.integer
      && new DecimalMax(this.value.decimal).validate(value);
}

export class Past implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.isBefore(value);
}

export class PastOrPresent implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.isBefore(value);
}

export class Future implements Validator<any> {
  message = 'invalid';
  validate = (value: any) => validator.isAfter(value);
}

export class Pattern implements Validator<string> {
  message = 'invalid';
  value: any;
  constructor(pattern: RegExp) {
    this.value = pattern;
  }
  validate = (value: any) => validator.matches(value, this.value);
}

// export class PastOrPresent implements Validator<any>
// export class FutureOrPresent implements Validator<any>
