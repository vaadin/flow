// @ts-nocheck

import MyBazModel from './MyBazModel';
import MyEntityIdModel from './MyEntityIdModel';
import MyEntity from './MyEntity';

import {ObjectModel,StringModel,NumberModel,ArrayModel,BooleanModel,Required,ModelType,getPropertyModelSymbol} from '@vaadin/form';

import {Email,Null,NotNull,NotEmpty,NotBlank,AssertTrue,AssertFalse,Negative,NegativeOrZero,Positive,PositiveOrZero,Size,Past,Future,Digits,Min,Max,Pattern,DecimalMin,DecimalMax} from '@vaadin/form';

/**
 * This module is generated from com.vaadin.flow.server.connect.generator.tsmodel.TsFormEndpoint.MyEntity.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 */
export default class MyEntityModel<T extends MyEntity = MyEntity> extends MyEntityIdModel<T> {
  static createEmptyValue: () => MyEntity;

  get assertFalse(): StringModel {
    return this[getPropertyModelSymbol]('assertFalse', StringModel, [false, new AssertFalse()]);
  }

  get assertTrue(): StringModel {
    return this[getPropertyModelSymbol]('assertTrue', StringModel, [false, new AssertTrue()]);
  }

  get bar(): MyBazModel {
    return this[getPropertyModelSymbol]('bar', MyBazModel, [false]);
  }

  get baz(): ArrayModel<ModelType<MyBazModel>, MyBazModel> {
    return this[getPropertyModelSymbol]('baz', ArrayModel, [false, MyBazModel, [false]]);
  }

  get bool(): BooleanModel {
    return this[getPropertyModelSymbol]('bool', BooleanModel, [false]);
  }

  get children(): ArrayModel<ModelType<MyEntityModel>, MyEntityModel> {
    return this[getPropertyModelSymbol]('children', ArrayModel, [false, MyEntityModel, [false]]);
  }

  get decimalMax(): NumberModel {
    return this[getPropertyModelSymbol]('decimalMax', NumberModel, [false, new DecimalMax({value:"0.01", inclusive:false})]);
  }

  get decimalMin(): NumberModel {
    return this[getPropertyModelSymbol]('decimalMin', NumberModel, [false, new DecimalMin("0.01")]);
  }

  get digits(): StringModel {
    return this[getPropertyModelSymbol]('digits', StringModel, [false, new Digits({integer:5, fraction:2})]);
  }

  get email(): StringModel {
    return this[getPropertyModelSymbol]('email', StringModel, [false, new Email({message:"foo"})]);
  }

  get entityMap(): ObjectModel<{ [key: string]: ModelType<MyBazModel>; }> {
    return this[getPropertyModelSymbol]('entityMap', ObjectModel, [false]);
  }

  get entityMatrix(): ArrayModel<ModelType<ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>, ArrayModel<ModelType<MyEntityModel>, MyEntityModel>> {
    return this[getPropertyModelSymbol]('entityMatrix', ArrayModel, [false, ArrayModel, [false, MyEntityModel, [false]]]);
  }

  get foo(): StringModel {
    return this[getPropertyModelSymbol]('foo', StringModel, [false]);
  }

  get future(): StringModel {
    return this[getPropertyModelSymbol]('future', StringModel, [false, new Future()]);
  }

  get isNull(): StringModel {
    return this[getPropertyModelSymbol]('isNull', StringModel, [false, new Null()]);
  }

  get list(): ArrayModel<string, StringModel> {
    return this[getPropertyModelSymbol]('list', ArrayModel, [false, StringModel, [false], new NotEmpty()]);
  }

  get localTime(): StringModel {
    return this[getPropertyModelSymbol]('localTime', StringModel, [false]);
  }

  get max(): NumberModel {
    return this[getPropertyModelSymbol]('max', NumberModel, [false, new Max(2)]);
  }

  get min(): NumberModel {
    return this[getPropertyModelSymbol]('min', NumberModel, [false, new Min({value:1, message:"foo"})]);
  }

  get negative(): NumberModel {
    return this[getPropertyModelSymbol]('negative', NumberModel, [false, new Negative()]);
  }

  get negativeOrCero(): NumberModel {
    return this[getPropertyModelSymbol]('negativeOrCero', NumberModel, [false, new NegativeOrZero()]);
  }

  get notBlank(): StringModel {
    return this[getPropertyModelSymbol]('notBlank', StringModel, [false, new NotBlank()]);
  }

  get notEmpty(): StringModel {
    return this[getPropertyModelSymbol]('notEmpty', StringModel, [false, new NotEmpty(), new NotNull()]);
  }

  get notNull(): StringModel {
    return this[getPropertyModelSymbol]('notNull', StringModel, [false, new NotNull()]);
  }

  get nullableEntity(): MyEntityModel {
    return this[getPropertyModelSymbol]('nullableEntity', MyEntityModel, [true]);
  }

  get nullableList(): ArrayModel<string, StringModel> {
    return this[getPropertyModelSymbol]('nullableList', ArrayModel, [true, StringModel, [true]]);
  }

  get nullableMatrix(): ArrayModel<ModelType<ArrayModel<string, StringModel>>, ArrayModel<string, StringModel>> {
    return this[getPropertyModelSymbol]('nullableMatrix', ArrayModel, [true, ArrayModel, [false, StringModel, [true]]]);
  }

  get nullableString(): StringModel {
    return this[getPropertyModelSymbol]('nullableString', StringModel, [true]);
  }

  get numberMatrix(): ArrayModel<ModelType<ArrayModel<number, NumberModel>>, ArrayModel<number, NumberModel>> {
    return this[getPropertyModelSymbol]('numberMatrix', ArrayModel, [false, ArrayModel, [false, NumberModel, [false]]]);
  }

  get optionalEntity(): MyEntityModel {
    return this[getPropertyModelSymbol]('optionalEntity', MyEntityModel, [true]);
  }

  get optionalList(): ArrayModel<string, StringModel> {
    return this[getPropertyModelSymbol]('optionalList', ArrayModel, [true, StringModel, [true]]);
  }

  get optionalMatrix(): ArrayModel<ModelType<ArrayModel<string, StringModel>>, ArrayModel<string, StringModel>> {
    return this[getPropertyModelSymbol]('optionalMatrix', ArrayModel, [true, ArrayModel, [false, StringModel, [true]]]);
  }

  get optionalString(): StringModel {
    return this[getPropertyModelSymbol]('optionalString', StringModel, [true]);
  }

  get past(): StringModel {
    return this[getPropertyModelSymbol]('past', StringModel, [false, new Past()]);
  }

  get pattern(): StringModel {
    return this[getPropertyModelSymbol]('pattern', StringModel, [false, new Pattern({regexp:"\\d+\\..+"})]);
  }

  get positive(): NumberModel {
    return this[getPropertyModelSymbol]('positive', NumberModel, [false, new Positive()]);
  }

  get positiveOrCero(): NumberModel {
    return this[getPropertyModelSymbol]('positiveOrCero', NumberModel, [false, new PositiveOrZero()]);
  }

  get size(): StringModel {
    return this[getPropertyModelSymbol]('size', StringModel, [false, new Size()]);
  }

  get size1(): StringModel {
    return this[getPropertyModelSymbol]('size1', StringModel, [false, new Size({min:1})]);
  }

  get stringArray(): ArrayModel<string, StringModel> {
    return this[getPropertyModelSymbol]('stringArray', ArrayModel, [false, StringModel, [false]]);
  }

  get stringMap(): ObjectModel<{ [key: string]: string; }> {
    return this[getPropertyModelSymbol]('stringMap', ObjectModel, [false]);
  }
}
