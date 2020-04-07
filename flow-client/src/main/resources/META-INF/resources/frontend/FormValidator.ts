import * as validator from 'validator';

// JSR380 equivalent (https://beanvalidation.org/2.0/spec/#builtinconstraints)
export const email = (value: string) => validator.isEmail(value);
export const isNull = (value: any) => value == null;
export const notNull = (value: any) => !isNull(value);
export const notEmpty = (value: any) => notNull(value) && !validator.isEmpty(value);
export const notBlank= (value: any) => notEmpty(value);
export const assertTrue = (value: any) => validator.isBoolean(String(value)) && String(value) === 'true';
export const assertFalse = (value: any) => !assertTrue(value);
export const min = (value: any, limit: number) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {min: limit})
export const max = (value: any, limit: number) => validator.isNumeric(String(value)) && validator.isFloat(String(value), {max: limit})

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









