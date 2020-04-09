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

// export const decimalMin
// export const decimalMax
// export const negative
// export const negativeOrCero
// export const positive
// export const positiveOrCero
// export const size
// export const digits
// export const past
// export const pastOrPresent
// export const future
// export const futureOrPresent
// export const pattern









