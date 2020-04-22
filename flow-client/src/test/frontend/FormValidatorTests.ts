const { suite, test} = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import { AssertFalse, AssertTrue, DecimalMin, DecimalMax, Digits, Email, Future, Max, Min, Negative, NegativeOrCero, NotBlank, NotEmpty, NotNull, Null, Past, Pattern, Positive, PositiveOrCero, Size } from "../../main/resources/META-INF/resources/frontend/FormValidator";

suite("Validator", () => {
  test("Email", () => {
    const validator = new Email()
    assert.isTrue(validator.validate('foo@vaadin.com'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate('foo'));
    assert.isFalse(validator.validate('foo@vaadin.c'));
    assert.isFalse(validator.validate('ñññ@vaadin.c'));
  });

  test("Null", () => {
    const validator = new Null();
    assert.isTrue(validator.validate(null));
    assert.isTrue(validator.validate(undefined));
    assert.isFalse(validator.validate(''));
  });

  test("NotNull", () => {
    const validator = new NotNull();
    assert.isTrue(validator.validate(''));
    assert.isFalse(validator.validate(null));
    assert.isFalse(validator.validate(undefined));
  });

  test("NotEmpty", () => {
    const validator = new NotEmpty();
    assert.isTrue(validator.validate('a'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate(undefined));
  });

  test("NotBlank", () => {
    const validator = new NotBlank();
    assert.isTrue(validator.validate('a'));
    assert.isFalse(validator.validate(''));
    assert.isFalse(validator.validate(undefined));
  });

  test("AssertTrue", () => {
    const validator = new AssertTrue();
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
    const validator = new Min(1);
    assert.isTrue(validator.validate(1));
    assert.isTrue(validator.validate(1.1));
    assert.isFalse(validator.validate(0.9));
  });

  test("Max", () => {
    const validator = new Max(1);
    assert.isTrue(validator.validate(1));
    assert.isTrue(validator.validate(0.9));
    assert.isFalse(validator.validate(1.1));
  });

  test("DecimalMin", () => {
    const validator = new DecimalMin(2);
    assert.isFalse(validator.validate(1));
    assert.isFalse(validator.validate(1.1));
    assert.isTrue(validator.validate(1.11));
    assert.isTrue(validator.validate(1.111));
  });

  test("DecimalMax", () => {
    const validator = new DecimalMax(2);
    assert.isTrue(validator.validate(1));
    assert.isTrue(validator.validate(1.1));
    assert.isTrue(validator.validate(1.11));
    assert.isFalse(validator.validate(1.111));
  });

  test("Negative", () => {
    const validator = new Negative();
    assert.isTrue(validator.validate(-1));
    assert.isTrue(validator.validate(-0.01));
    assert.isFalse(validator.validate(0));
    assert.isFalse(validator.validate(1));
  });


  test("NegativeOrCero", () => {
    const validator = new NegativeOrCero();
    assert.isTrue(validator.validate(-1));
    assert.isTrue(validator.validate(-0.01));
    assert.isTrue(validator.validate(0));
    assert.isFalse(validator.validate(1));
  });

  test("Positive", () => {
    const validator = new Positive();
    assert.isFalse(validator.validate(-1));
    assert.isFalse(validator.validate(-0.01));
    assert.isFalse(validator.validate(0));
    assert.isTrue(validator.validate(0.01));
  });

  test("PositiveOrCero", () => {
    const validator = new PositiveOrCero();
    assert.isFalse(validator.validate(-1));
    assert.isFalse(validator.validate(-0.01));
    assert.isTrue(validator.validate(0));
    assert.isTrue(validator.validate(0.01));
  });


  test("Size", () => {
    const validator = new Size(2, 4);
    assert.isFalse(validator.validate(""));
    assert.isFalse(validator.validate("a"));
    assert.isTrue(validator.validate("aa"));
    assert.isTrue(validator.validate("aaa"));
  });

  test("Digits", () => {
    const validator = new Digits(2, 3);
    assert.isTrue(validator.validate("11.111"));
    assert.isFalse(validator.validate("1.1"));
    assert.isFalse(validator.validate("111.1111"));
  });

  test("Past", () => {
    const validator = new Past();
    assert.isTrue(validator.validate("2019-12-31"));
    assert.isFalse(validator.validate(String(new Date())));
    assert.isFalse(validator.validate("3000-01-01"));
  });

  // test("PastOrPresent", () => {
  // });

  test("Future", () => {
    const validator = new Future();
    assert.isFalse(validator.validate("2019-12-31"));
    assert.isFalse(validator.validate(String(new Date())));
    assert.isTrue(validator.validate("3000-01-01"));
  });

  // test("FutureOrPresent", () => {
  // });

  test("Pattern", () => {
    const validator = new Pattern(/^(\+\d+)?([ -]?\d+){4,14}$/);
    assert.isFalse(validator.validate(""));
    assert.isFalse(validator.validate("123"));
    assert.isFalse(validator.validate("abcdefghijk"));
    assert.isTrue(validator.validate("+35 123 456 789"));
    assert.isTrue(validator.validate("123 456 789"));
    assert.isTrue(validator.validate("123-456-789"));
  });

});

