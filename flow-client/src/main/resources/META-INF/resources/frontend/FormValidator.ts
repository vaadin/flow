import * as validator from 'validator';

export type ValidationCallback<T> = (value: T) => boolean | Promise<boolean>;

export interface Validator<T> {
  validate: ValidationCallback<T>,
  message: string,
  value?: any
}

// JSR380 equivalent (https://beanvalidation.org/2.0/spec/#builtinconstraints)
export class Email implements Validator<string> {
  validate = (value: string) => validator.isEmail(value);
  message = 'invalid';
}
export class Null implements Validator<any> {
  validate = (value: any) => value == null;
  message = 'invalid';
}
export class NotNull implements Validator<any> {
  validate = (value: any) => !new Null().validate(value);
  message = 'invalid';
}
export class NotEmpty implements Validator<any> {
  validate = (value: any) => new NotNull().validate(value) && !validator.isEmpty(value);
  message = 'invalid';
}
export class NotBlank implements Validator<any> {
  validate = (value: any) => new NotEmpty().validate(value);
  message = 'invalid';
}
export class AssertTrue implements Validator<any> {
  validate = (value: any) => validator.isBoolean(String(value)) && String(value) === 'true';
  message = 'invalid';
}
export class AssertFalse implements Validator<any> {
  validate = (value: any) => !new AssertTrue().validate(value);
  message = 'invalid';
}
export class Min implements Validator<any> {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {min: this.value})
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
}
export class Max implements Validator<any> {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {max: this.value})
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
}

export class DecimalMin implements Validator<any> {
  validate = (value: any) => validator.isDecimal(`${value}`, {decimal_digits: `${this.value},`, force_decimal: true});
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
}
export class DecimalMax implements Validator<any> {
  validate = (value: any) => validator.isDecimal(`${value}`, {decimal_digits: `0,${this.value}`});
  message = 'invalid';
  value: number;
  constructor(value: number) {
    this.value = value;
  }
}

export class Negative implements Validator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) < 0;
  message = 'invalid';
}
export class NegativeOrCero implements Validator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) <= 0;
  message = 'invalid';
}

export class Positive implements Validator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) > 0;
  message = 'invalid';
}

export class PositiveOrCero implements Validator<any> {
  validate = (value: any) => validator.toFloat(`${value}`) >= 0;
  message = 'invalid';
}

export class Size implements Validator<string> {
  validate = (value: string) => validator.isLength(value, this.value);
  message = 'invalid';
  value: any;
  constructor(min: number, max: number) {
    this.value = {min, max};
  }
}

export class Digits implements Validator<string> {
  validate = (value: any) =>
      String(validator.toFloat(`${value}`)).replace(/(.*)\.\d+/, "$1").length === this.value.integer
      && new DecimalMax(this.value.decimal).validate(value);
  message = 'invalid';
  value: any;
  constructor(integer: number, decimal: number) {
    this.value = {integer, decimal};
  }
}

export class Past implements Validator<any> {
  validate = (value: any) => validator.isBefore(value);
  message = 'invalid';
}

export class PastOrPresent implements Validator<any> {
  validate = (value: any) => validator.isBefore(value);
  message = 'invalid';
}

export class Future implements Validator<any> {
  validate = (value: any) => validator.isAfter(value);
  message = 'invalid';
}

export class Pattern implements Validator<string> {
  validate = (value: any) => validator.matches(value, this.value);
  message = 'invalid';
  value: any;
  constructor(pattern: RegExp) {
    this.value = pattern;
  }
}

// export class PastOrPresent implements Validator<any>
// export class FutureOrPresent implements Validator<any>
