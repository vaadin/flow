const { suite, test} = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import * as val  from "../../main/resources/META-INF/resources/frontend/FormValidator";

suite("Validator", () => {

  test("email", () => {
    assert.isTrue(val.email('foo@vaadin.com'));
    assert.isFalse(val.email(''));
    assert.isFalse(val.email('foo'));
    assert.isFalse(val.email('foo@vaadin.c'));
    assert.isFalse(val.email('ñññ@vaadin.c'));
  });

  test("isNull", () => {
    assert.isTrue(val.isNull(null));
    assert.isTrue(val.isNull(undefined));
    assert.isFalse(val.isNull(''));
  });

  test("notNull", () => {
    assert.isTrue(val.notNull(''));
    assert.isFalse(val.notNull(null));
    assert.isFalse(val.notNull(undefined));
  });

  test("notEmpty", () => {
    assert.isTrue(val.notEmpty('a'));
    assert.isFalse(val.notEmpty(''));
    assert.isFalse(val.notEmpty(undefined));
  });

  test("notBlank=", () => {
    assert.isTrue(val.notBlank('a'));
    assert.isFalse(val.notBlank(''));
    assert.isFalse(val.notBlank(undefined));
  });

  test("assertTrue", () => {
    assert.isTrue(val.assertTrue('true'));
    assert.isTrue(val.assertTrue(true));
    assert.isFalse(val.assertTrue('a'));
    assert.isFalse(val.assertTrue(false));
    assert.isFalse(val.assertTrue('false'));
    assert.isFalse(val.assertTrue(''));
    assert.isFalse(val.assertTrue(undefined));
    assert.isFalse(val.assertTrue(null));
    assert.isFalse(val.assertTrue(1));
    assert.isFalse(val.assertTrue(0));
  });

  test("assertFalse", () => {
    assert.isTrue(val.assertFalse('false'));
    assert.isTrue(val.assertFalse(false));
    assert.isTrue(val.assertFalse('a'));
    assert.isTrue(val.assertFalse('foo'));
    assert.isTrue(val.assertFalse(''));
    assert.isTrue(val.assertFalse(undefined));
    assert.isTrue(val.assertFalse(null));
    assert.isFalse(val.assertFalse('true'));
    assert.isFalse(val.assertFalse(true));
  });

  test("min", () => {
    assert.isTrue(val.min(1, 1));
    assert.isTrue(val.min(1.1, 1));
    assert.isFalse(val.min(0.9, 1));
  });

  test("max", () => {
    assert.isTrue(val.max(1, 1));
    assert.isTrue(val.max(0.9, 1));
    assert.isFalse(val.max(1.1, 1));
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

