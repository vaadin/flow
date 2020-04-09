const { suite, test} = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import {Email, Null, NotNull, NotEmpty, NotBlank, AssertTrue, AssertFalse, Min, Max} from "../../main/resources/META-INF/resources/frontend/FormValidator";

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

  // test("decimalMin", () => {
  // });

  // test("decimalMax", () => {
  // });

  // test("negative", () => {
  // });

  // test("negativeOrCero", () => {
  // });

  // test("positive", () => {
  // });

  // test("positiveOrCero", () => {
  // });

  // test("size", () => {
  // });

  // test("digits", () => {
  // });

  // test("past", () => {
  // });

  // test("pastOrPresent", () => {
  // });

  // test("future", () => {
  // });

  // test("futureOrPresent", () => {
  // });

  // test("pattern", () => {
  // });  

});

