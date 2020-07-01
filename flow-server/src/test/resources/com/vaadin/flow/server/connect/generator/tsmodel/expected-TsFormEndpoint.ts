import MyBazModel from './MyBazModel';
import MyEntityIdModel from './MyEntityIdModel';
import MyEntity from './MyEntity';

// @ts-ignore
import {ObjectModel,StringModel,NumberModel,ArrayModel,BooleanModel,Required,ModelType} from '@vaadin/form';

// @ts-ignore
import {Email,Null,NotNull,NotEmpty,NotBlank,AssertTrue,AssertFalse,Negative,NegativeOrZero,Positive,PositiveOrZero,Size,Past,PastOrPresent,Future,FutureOrPresent,Digits,Min,Max,Pattern,DecimalMin,DecimalMax} from '@vaadin/form';

/**
 * This module is generated from com.vaadin.flow.server.connect.generator.tsmodel.TsFormEndpoint.MyEntity.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 */
export default class MyEntityModel<T extends MyEntity = MyEntity> extends MyEntityIdModel<T> {
  static createEmptyValue: () => MyEntity;
  public readonly assertFalse = new StringModel(this, 'assertFalse', new AssertFalse());
  public readonly assertTrue = new StringModel(this, 'assertTrue', new AssertTrue());
  public readonly bar = new MyBazModel(this, 'bar');
  public readonly baz = new ArrayModel<ModelType<MyBazModel>, MyBazModel>(this, 'baz', MyBazModel, []);
  public readonly bool = new BooleanModel(this, 'bool');
  public readonly children = new ArrayModel<ModelType<MyEntityModel>, MyEntityModel>(this, 'children', MyEntityModel, []);
  public readonly decimalMax = new NumberModel(this, 'decimalMax', new DecimalMax({value:"0.01", inclusive:false}));
  public readonly decimalMin = new NumberModel(this, 'decimalMin', new DecimalMin("0.01"));
  public readonly digits = new StringModel(this, 'digits', new Digits({integer:5, fraction:2}));
  public readonly email = new StringModel(this, 'email', new Email({message:"foo"}));
  public readonly entityMap = new ObjectModel<{ [key: string]: ModelType<MyBazModel>; }>(this, 'entityMap');
  public readonly entityMatrix = new ArrayModel<ModelType<ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>, ArrayModel<ModelType<MyEntityModel>, MyEntityModel>>(this, 'entityMatrix', ArrayModel, [MyEntityModel, []]);
  public readonly foo = new StringModel(this, 'foo');
  public readonly future = new StringModel(this, 'future', new Future());
  public readonly futureOrPresent = new ObjectModel(this, 'futureOrPresent', new FutureOrPresent());
  public readonly isNull = new StringModel(this, 'isNull', new Null());
  public readonly list = new ArrayModel(this, 'list', StringModel, [], new NotEmpty());
  public readonly max = new NumberModel(this, 'max', new Max(2));
  public readonly min = new NumberModel(this, 'min', new Min({value:1, message:"foo"}));
  public readonly negative = new NumberModel(this, 'negative', new Negative());
  public readonly negativeOrCero = new NumberModel(this, 'negativeOrCero', new NegativeOrZero());
  public readonly notBlank = new StringModel(this, 'notBlank', new NotBlank());
  public readonly notEmpty = new StringModel(this, 'notEmpty', new NotEmpty(), new NotNull());
  public readonly notNull = new StringModel(this, 'notNull', new NotNull());
  public readonly numberMatrix = new ArrayModel(this, 'numberMatrix', ArrayModel, [NumberModel, []]);
  public readonly past = new StringModel(this, 'past', new Past());
  public readonly pastOrPresent = new ObjectModel(this, 'pastOrPresent', new PastOrPresent());
  public readonly pattern = new StringModel(this, 'pattern', new Pattern({regexp:"\\d+\\..+"}));
  public readonly positive = new NumberModel(this, 'positive', new Positive());
  public readonly positiveOrCero = new NumberModel(this, 'positiveOrCero', new PositiveOrZero());
  public readonly size = new StringModel(this, 'size', new Size());
  public readonly size1 = new StringModel(this, 'size1', new Size({min:1}));
  public readonly stringArray = new ArrayModel(this, 'stringArray', StringModel, []);
  public readonly stringMap = new ObjectModel<{ [key: string]: string; }>(this, 'stringMap');
}
