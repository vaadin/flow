/* tslint:disable:max-classes-per-file */

import * as validator from 'validator';
import { Validator } from './Validation';

interface ValidatorAttributes {
  message?: string;
}
interface ValueNumberAttributes extends ValidatorAttributes {
  value: number | string;
}
interface DigitAttributes extends ValidatorAttributes {
  integer: number;
  fraction: number;
}
interface SizeAttributes extends ValidatorAttributes {
  min?: number;
  max?: number;
}
interface PatternAttributes extends ValidatorAttributes {
  regexp: RegExp | string;
}
interface DecimalAttributes extends ValueNumberAttributes {
  inclusive?: boolean;
}

abstract class AbstractValidator<T> implements Validator<T> {
  message = 'invalid';
  impliesRequired = false;
  constructor(attrs?: ValidatorAttributes) {
    if (attrs && attrs.message) {
      this.message = attrs.message;
    }
  }
  abstract validate(value: T): boolean | Promise<boolean>;
}

export class Required<T> extends AbstractValidator<T> {
  impliesRequired = true;
  validate(value: T){
    if (typeof value === 'string' || Array.isArray(value)) {
      return value.length > 0;
    } else if (typeof value === 'number') {
      return Number.isFinite(value);
    }
    return value !== undefined;
  }
}

abstract class ValueNumberValidator<T> extends AbstractValidator<T> {
  value: number;
  constructor(attrs: ValueNumberAttributes | number | string) {
    super(typeof attrs === 'number' || typeof attrs === 'string' ? {} : attrs);
    const val = typeof attrs === 'object' ? attrs.value : attrs;
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
export class NotNull extends Required<any> {
  validate(value: any) {
    return !new Null().validate(value);
  }
}
export class NotEmpty extends Required<any> {
  validate = (value: any) => {
    return super.validate(value) && new NotNull().validate(value) && value.length > 0;
  }
}
export class NotBlank extends Required<any> {
  validate = (value: any) => new NotEmpty().validate(value);
}
export class AssertTrue extends AbstractValidator<any> {
  validate = (value: any) => validator.isBoolean(String(value)) && String(value) === 'true';
}
export class AssertFalse extends AbstractValidator<any> {
  validate = (value: any) => !new AssertTrue().validate(value);
}
export class Min extends ValueNumberValidator<any> {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), { min: this.value });
}
export class Max extends ValueNumberValidator<any> {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), { max: this.value });
}
export class DecimalMin extends ValueNumberValidator<any> {
  inclusive: boolean;
  constructor(attrs: DecimalAttributes | string | number) {
    super(attrs);
    this.inclusive = typeof attrs !== 'object' || attrs.inclusive !== false;
  }
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), { [this.inclusive ? 'min' : 'gt']: this.value });
}
export class DecimalMax extends DecimalMin {
  validate = (value: any) => validator.isNumeric(String(value)) && validator.isFloat(String(value), { [this.inclusive ? 'max' : 'lt']: this.value })
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
  min: number;
  max: number;
  constructor(attrs: SizeAttributes) {
    super(attrs);
    this.min = attrs.min || 0;
    this.max = attrs.max || Number.MAX_SAFE_INTEGER;
    if (this.min > 0) {
      this.impliesRequired = true;
    }
  }
  validate = (value: string) => {
    if (this.min && this.min > 0 && !new Required().validate(value)) {
      return false;
    }
    return validator.isLength(value, this.min, this.max);
  }
}

export class Digits extends AbstractValidator<string> {
  integer: number;
  fraction: number;
  constructor(attrs: DigitAttributes) {
    super(attrs);
    this.integer = attrs.integer;
    this.fraction = attrs.fraction;
  }
  validate = (value: any) =>
    String(validator.toFloat(`${value}`)).replace(/(.*)\.\d+/, "$1").length === this.integer
    && validator.isDecimal(`${value}`, { decimal_digits: `0,${this.fraction}` })
}

export class Past extends AbstractValidator<any> {
  validate = (value: any) => validator.isBefore(value);
}
export class PastOrPresent extends AbstractValidator<any> {
  validate = () => { throw new Error('Form Validator for PastOrPresent not implemented yet') };
}
export class Future extends AbstractValidator<any> {
  validate = (value: any) => validator.isAfter(value);
}
export class FutureOrPresent extends AbstractValidator<any> {
  validate = () => { throw new Error('Form Validator for FutureOrPresent not implemented yet') };
}

export class Pattern extends AbstractValidator<string> {
  regexp: RegExp;
  constructor(attrs: PatternAttributes | string | RegExp) {
    super(typeof attrs === 'string' || attrs instanceof RegExp ? {} : attrs);
    this.regexp = typeof attrs === 'string' ? new RegExp(attrs)
      : attrs instanceof RegExp ? attrs
        : typeof attrs.regexp === 'string' ? new RegExp(attrs.regexp) : attrs.regexp;
  }
  validate = (value: any) => validator.matches(value, this.regexp);
}
