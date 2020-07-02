const { suite, test} = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import {
  AssertFalse,
  AssertTrue,
  DecimalMax,
  DecimalMin,
  Digits,
  Email,
  Future,
  FutureOrPresent,
  Max,
  Min,
  Negative,
  NegativeOrZero,
  NotBlank,
  NotEmpty,
  NotNull,
  Null,
  Past,
  PastOrPresent,
  Pattern,
  Positive,
  PositiveOrZero,
  Required,
  Size
}
  from "../../../main/resources/META-INF/resources/frontend/form";

suite("form/Validators", () => {

  test("custom error message", () => {
    assert.equal(new Size({min: 1, max: 2}).message,
      'size must be between 1 and 2',
      'when no custom error message is provided, the default error message should be used');
    assert.equal(new Size({min: 1, max: 2, message: 'Please enter something with the size between 1 and 2'}).message,
      'Please enter something with the size between 1 and 2',
      'when a custom error message is provided, it should be used instead of the default one');
  });

  test("Required", () => {
    const validator = new Required();
    assert.isTrue(validator.impliesRequired);
    assert.isTrue(validator.validate('foo'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate(undefined));
    assert.isTrue(validator.validate(0));
  });

  test("Email", () => {
    const validator = new Email();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate('foo@vaadin.com'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate('foo'));
    assert.isFalse(validator.validate('foo@vaadin.c'));
    assert.isFalse(validator.validate('ñññ@vaadin.c'));
  });

  test("Null", () => {
    const validator = new Null();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(null));
    assert.isTrue(validator.validate(undefined));
    assert.isFalse(validator.validate(''));
  });

  test("NotNull", () => {
    const validator = new NotNull();
    assert.isTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(''));
    assert.isFalse(validator.validate(null));
    assert.isFalse(validator.validate(undefined));
  });

  test("NotEmpty", () => {
    const validator = new NotEmpty();
    assert.isTrue(validator.impliesRequired);
    assert.isTrue(validator.validate('a'));
    assert.isTrue(validator.validate(['a']));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate(undefined));
    assert.isFalse(validator.validate([]));
  });

  test("NotBlank", () => {
    const validator = new NotBlank();
    assert.isTrue(validator.impliesRequired);
    assert.isTrue(validator.validate('a'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate(undefined));
  });

  test("AssertTrue", () => {
    const validator = new AssertTrue();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate('true'));
    assert.isTrue(validator.validate(true));
    assert.isFalse(validator.validate('a'));
    assert.isFalse(validator.validate(false));
    assert.isFalse(validator.validate('false'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate(undefined));
    assert.isFalse(validator.validate(null));
    assert.isFalse(validator.validate(1));
    assert.isFalse(validator.validate(0));
  });

  test("AssertFalse", () => {
    const validator = new AssertFalse();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate('false'));
    assert.isTrue(validator.validate(false));
    assert.isTrue(validator.validate('a'));
    assert.isTrue(validator.validate('foo'));
    assert.isTrue(validator.validate(''));
    assert.isTrue(validator.validate(undefined));
    assert.isTrue(validator.validate(null));
    assert.isFalse(validator.validate('true'));
    assert.isFalse(validator.validate(true));
  });

  test("Min", () => {
    let validator = new Min(1);
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(1));
    assert.isTrue(validator.validate(1.1));
    assert.isFalse(validator.validate(0.9));
    validator = new Min({message: 'foo', value: 1});
    assert.isTrue(validator.validate(1));
    assert.isTrue(validator.validate(1.1));
    assert.isFalse(validator.validate(0.9));
  });

  test("Max", () => {
    const validator = new Max(1);
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(1));
    assert.isTrue(validator.validate(0.9));
    assert.isFalse(validator.validate(1.1));
  });

  test("DecimalMin", () => {
    let validator = new DecimalMin("30.1");
    assert.isNotTrue(validator.impliesRequired);
    assert.isFalse(validator.validate(1));
    assert.isTrue(validator.validate(30.1));
    assert.isTrue(validator.validate(30.2));
    assert.isTrue(validator.validate("30.2"));
    validator = new DecimalMin({value: "30.1", inclusive: false});
    assert.isFalse(validator.validate(30.1));
  });

  test("DecimalMax", () => {
    let validator = new DecimalMax("30.1");
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(30));
    assert.isTrue(validator.validate(30.1));
    assert.isFalse(validator.validate(30.2));
    validator = new DecimalMin({value: "30.1", inclusive: false});
    assert.isFalse(validator.validate(30.1));
  });

  test("Negative", () => {
    const validator = new Negative();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(-1));
    assert.isTrue(validator.validate(-0.01));
    assert.isFalse(validator.validate(0));
    assert.isFalse(validator.validate(1));
  });


  test("NegativeOrZero", () => {
    const validator = new NegativeOrZero();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate(-1));
    assert.isTrue(validator.validate(-0.01));
    assert.isTrue(validator.validate(0));
    assert.isFalse(validator.validate(1));
  });

  test("Positive", () => {
    const validator = new Positive();
    assert.isNotTrue(validator.impliesRequired);
    assert.isFalse(validator.validate(-1));
    assert.isFalse(validator.validate(-0.01));
    assert.isFalse(validator.validate(0));
    assert.isTrue(validator.validate(0.01));
  });

  test("PositiveOrZero", () => {
    const validator = new PositiveOrZero();
    assert.isNotTrue(validator.impliesRequired);
    assert.isFalse(validator.validate(-1));
    assert.isFalse(validator.validate(-0.01));
    assert.isTrue(validator.validate(0));
    assert.isTrue(validator.validate(0.01));
  });


  test("Size", () => {
    const validator = new Size({min: 2, max: 4});
    assert.isTrue(validator.impliesRequired);
    assert.isFalse(validator.validate(""));
    assert.isFalse(validator.validate("a"));
    assert.isTrue(validator.validate("aa"));
    assert.isTrue(validator.validate("aaa"));
    const noMinValidator = new Size({max: 3});
    assert.isNotTrue(noMinValidator.impliesRequired);
    const minZeroValidator = new Size({min: 0, max: 3});
    assert.isNotTrue(minZeroValidator.impliesRequired);
  });

  test("Digits", () => {
    const validator = new Digits({integer:2, fraction:3});
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate("11.111"));
    assert.isFalse(validator.validate("1.1"));
    assert.isFalse(validator.validate("111.1111"));
  });

  test("Past", () => {
    const validator = new Past();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate("2019-12-31"), 'past');
    assert.isFalse(validator.validate(String(new Date())), 'present');
    assert.isFalse(validator.validate("3000-01-01"), 'future');
  });

  test("PastOrPresent", () => {
    const validator = new PastOrPresent();
    assert.isNotTrue(validator.impliesRequired);
    assert.isTrue(validator.validate("2019-12-31"), 'past');
    assert.isTrue(validator.validate(String(new Date())), 'present');
    assert.isFalse(validator.validate("3000-01-01"), 'future');
  });

  test("Future", () => {
    const validator = new Future();
    assert.isNotTrue(validator.impliesRequired);
    assert.isFalse(validator.validate("2019-12-31"), 'past');
    assert.isFalse(validator.validate(String(new Date())), 'present');
    assert.isTrue(validator.validate("3000-01-01"), 'future');
  });

  test("FutureOrPresent", () => {
    const validator = new FutureOrPresent();
    assert.isNotTrue(validator.impliesRequired);
    assert.isFalse(validator.validate("2019-12-31"), 'past');
    assert.isTrue(validator.validate(String(new Date())), 'present');
    assert.isTrue(validator.validate("3000-01-01"), 'future');
  });

  test("Pattern", () => {
    let validator = new Pattern(/^(\+\d+)?([ -]?\d+){4,14}$/);
    assert.isNotTrue(validator.impliesRequired);
    assert.isFalse(validator.validate(""));
    assert.isFalse(validator.validate("123"));
    assert.isFalse(validator.validate("abcdefghijk"));
    assert.isTrue(validator.validate("+35 123 456 789"));
    assert.isTrue(validator.validate("123 456 789"));
    assert.isTrue(validator.validate("123-456-789"));
    validator = new Pattern("\\d+");
    assert.isTrue(validator.validate("1"));
    assert.isFalse(validator.validate("a"));
    validator = new Pattern({regexp: "\\w+\\\\"});
    assert.isFalse(validator.validate("a"));
    assert.isTrue(validator.validate("a\\"));
    validator = new Pattern({regexp: /\w+\\/});
    assert.isFalse(validator.validate("a"));
    assert.isTrue(validator.validate('a\\'));
  });
});

