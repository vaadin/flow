import MyBazModel from './MyBazModel';
import MyEntityIdModel from './MyEntityIdModel';
import MyEntity from './MyEntity';

// @ts-ignore
import {ObjectModel,StringModel,NumberModel,ArrayModel,BooleanModel,Required,ModelType,getPropertyModel} from '@vaadin/form';

// @ts-ignore
import {Email,Null,NotNull,NotEmpty,NotBlank,AssertTrue,AssertFalse,Negative,NegativeOrZero,Positive,PositiveOrZero,Size,Past,Future,Digits,Min,Max,Pattern,DecimalMin,DecimalMax} from '@vaadin/form';

/**
 * This module is generated from com.vaadin.flow.server.connect.generator.tsmodel.TsFormEndpoint.MyEntity.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 */
export default class MyEntityModel<T extends MyEntity = MyEntity> extends MyEntityIdModel<T> {
  static createEmptyValue: () => MyEntity;

  get assertFalse(): StringModel {
    return this[getPropertyModel]('assertFalse', StringModel, [false, new AssertFalse()]);
  }

  get assertTrue(): StringModel {
    return this[getPropertyModel]('assertTrue', StringModel, [false, new AssertTrue()]);
  }

  get bar(): MyBazModel {
    return this[getPropertyModel]('bar', MyBazModel, [false]);
  }

  get baz(): ArrayModel<ModelType<MyBazModel>, MyBazModel> {
    return this[getPropertyModel]('baz', ArrayModel, [false, MyBazModel, [false]]);
  }

  get bool(): BooleanModel {
    return this[getPropertyModel]('bool', BooleanModel, [false]);
  }

  get children(): ArrayModel<ModelType<MyEntityModel>, MyEntityModel> {
    return this[getPropertyModel]('children', ArrayModel, [false, MyEntityModel, [false]]);
  }

  get decimalMax(): NumberModel {
    return this[getPropertyModel]('decimalMax', NumberModel, [false, new DecimalMax({value:"0.01", inclusive:false})]);
  }

  get decimalMin(): NumberModel {
    return this[getPropertyModel]('decimalMin', NumberModel, [false, new DecimalMin("0.01")]);
  }

  get digits(): StringModel {
    return this[getPropertyModel]('digits', StringModel, [false, new Digits({integer:5, fraction:2})]);
  }

  get email(): StringModel {
    return this[getPropertyModel]('email', StringModel, [false, new Email({message:"foo"})]);
  }

  get entityMap(): ObjectModel<{ [key: string]: ModelType<MyBazModel>; }> {
    return this[getPropertyModel]('entityMap', ObjectModel, [false]);
  }

  get entityMatrix(): ArrayModel<ModelType<ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>, ArrayModel<ModelType<MyEntityModel>, MyEntityModel>> {
    return this[getPropertyModel]('entityMatrix', ArrayModel, [false, ArrayModel, [false, MyEntityModel, [false]]]);
  }

  get foo(): StringModel {
    return this[getPropertyModel]('foo', StringModel, [false]);
  }

  get future(): StringModel {
    return this[getPropertyModel]('future', StringModel, [false, new Future()]);
  }

  get isNull(): StringModel {
    return this[getPropertyModel]('isNull', StringModel, [false, new Null()]);
  }

  get list(): ArrayModel<string, StringModel> {
    return this[getPropertyModel]('list', ArrayModel, [false, StringModel, [false], new NotEmpty()]);
  }

  get max(): NumberModel {
    return this[getPropertyModel]('max', NumberModel, [false, new Max(2)]);
  }

  get min(): NumberModel {
    return this[getPropertyModel]('min', NumberModel, [false, new Min({value:1, message:"foo"})]);
  }

  get negative(): NumberModel {
    return this[getPropertyModel]('negative', NumberModel, [false, new Negative()]);
  }

  get negativeOrCero(): NumberModel {
    return this[getPropertyModel]('negativeOrCero', NumberModel, [false, new NegativeOrZero()]);
  }

  get notBlank(): StringModel {
    return this[getPropertyModel]('notBlank', StringModel, [false, new NotBlank()]);
  }

  get notEmpty(): StringModel {
    return this[getPropertyModel]('notEmpty', StringModel, [false, new NotEmpty(), new NotNull()]);
  }

  get notNull(): StringModel {
    return this[getPropertyModel]('notNull', StringModel, [false, new NotNull()]);
  }

  get nullableEntity(): MyEntityModel {
    return this[getPropertyModel]('nullableEntity', MyEntityModel, [true]);
  }

  get nullableList(): ArrayModel<string, StringModel> {
    return this[getPropertyModel]('nullableList', ArrayModel, [true, StringModel, [true]]);
  }

  get nullableMatrix(): ArrayModel<ModelType<ArrayModel<string, StringModel>>, ArrayModel<string, StringModel>> {
    return this[getPropertyModel]('nullableMatrix', ArrayModel, [true, ArrayModel, [false, StringModel, [true]]]);
  }

  get nullableString(): StringModel {
    return this[getPropertyModel]('nullableString', StringModel, [true]);
  }

  get numberMatrix(): ArrayModel<ModelType<ArrayModel<number, NumberModel>>, ArrayModel<number, NumberModel>> {
    return this[getPropertyModel]('numberMatrix', ArrayModel, [false, ArrayModel, [false, NumberModel, [false]]]);
  }

  get optionalEntity(): MyEntityModel {
    return this[getPropertyModel]('optionalEntity', MyEntityModel, [true]);
  }

  get optionalList(): ArrayModel<string, StringModel> {
    return this[getPropertyModel]('optionalList', ArrayModel, [true, StringModel, [true]]);
  }

  get optionalMatrix(): ArrayModel<ModelType<ArrayModel<string, StringModel>>, ArrayModel<string, StringModel>> {
    return this[getPropertyModel]('optionalMatrix', ArrayModel, [true, ArrayModel, [false, StringModel, [true]]]);
  }

  get optionalString(): StringModel {
    return this[getPropertyModel]('optionalString', StringModel, [true]);
  }

  get past(): StringModel {
    return this[getPropertyModel]('past', StringModel, [false, new Past()]);
  }

  get pattern(): StringModel {
    return this[getPropertyModel]('pattern', StringModel, [false, new Pattern({regexp:"\\d+\\..+"})]);
  }

  get positive(): NumberModel {
    return this[getPropertyModel]('positive', NumberModel, [false, new Positive()]);
  }

  get positiveOrCero(): NumberModel {
    return this[getPropertyModel]('positiveOrCero', NumberModel, [false, new PositiveOrZero()]);
  }

  get size(): StringModel {
    return this[getPropertyModel]('size', StringModel, [false, new Size()]);
  }

  get size1(): StringModel {
    return this[getPropertyModel]('size1', StringModel, [false, new Size({min:1})]);
  }

  get stringArray(): ArrayModel<string, StringModel> {
    return this[getPropertyModel]('stringArray', ArrayModel, [false, StringModel, [false]]);
  }

  get stringMap(): ObjectModel {
    return this[getPropertyModel]('stringMap', ObjectModel, [false]);
  }
}
