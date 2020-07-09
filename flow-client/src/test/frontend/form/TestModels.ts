/* tslint:disable:max-classes-per-file */
// API to test
import {
  ArrayModel,
  BooleanModel,
  getKeyModelSymbol,
  ModelConstructor,
  NumberModel,
  ObjectModel,
  Pattern,
  Positive,
  Required,
  Size,
  StringModel,
} from "../../../main/resources/META-INF/resources/frontend/form";

export interface IdEntity {
  idString: string;
}
export class IdEntityModel<T extends IdEntity = IdEntity> extends ObjectModel<T> {
  static createEmptyValue: () => IdEntity;
  get idString(): StringModel {
    return this[getKeyModelSymbol]('idString', StringModel, []);
  }
}

export interface Product extends IdEntity {
  description: string;
  price: number;
  isInStock: boolean;
}
export class ProductModel<T extends Product = Product> extends IdEntityModel<T> {
  static createEmptyValue: () => Product;

  get description() {
    return this[getKeyModelSymbol]('description', StringModel, [new Required()]);
  }

  get price() {
    return this[getKeyModelSymbol]('price', NumberModel, [new Positive()]);
  }

  get isInStock() {
    return this[getKeyModelSymbol]('isInStock', BooleanModel, []);
  }
}

interface Customer extends IdEntity {
  fullName: string;
  nickName: string;
}
export class CustomerModel<T extends Customer = Customer> extends IdEntityModel<T> {
  static createEmptyValue: () => Customer;

  get fullName() {
    return this[getKeyModelSymbol]('fullName', StringModel, [new Size({min: 4}), new Required()]) as StringModel;
  }

  get nickName() {
    return this[getKeyModelSymbol]('nickName', StringModel, [new Pattern("....*")]) as StringModel;
  }
}

export interface Order extends IdEntity {
  customer: Customer;
  notes: string;
  priority: number;
  products: ReadonlyArray<Product>;
}
export class OrderModel<T extends Order = Order> extends IdEntityModel<T> {
  static createEmptyValue: () => Order;

  get customer(): CustomerModel {
    return this[getKeyModelSymbol]('customer', CustomerModel, [new Required()]);
  }

  get notes(): StringModel {
    return this[getKeyModelSymbol]('notes', StringModel, [new Required()]);
  }

  get priority(): NumberModel {
    return this[getKeyModelSymbol]('priority', NumberModel, []);
  }

  get products(): ArrayModel<Product, ProductModel> {
    return this[getKeyModelSymbol]('products', ArrayModel as ModelConstructor<ReadonlyArray<Product>, ArrayModel<Product, ProductModel>>, [ProductModel, []]);
  }
}

export interface TestEntity {
  fieldString: string;
  fieldNumber: number;
  fieldBoolean: boolean;
  fieldObject: object;
  fieldArrayString: string[];
  fieldArrayModel: IdEntity[];
  fieldMatrixNumber: number[][];
}
export class TestModel<T extends TestEntity = TestEntity> extends ObjectModel<T> {
  static createEmptyValue: () => TestEntity;

  get fieldString() {
    return this[getKeyModelSymbol]('fieldString', StringModel, []) as StringModel;
  }

  get fieldNumber() {
    return this[getKeyModelSymbol]('fieldNumber', NumberModel, []) as NumberModel;
  }

  get fieldBoolean() {
    return this[getKeyModelSymbol]('fieldBoolean', BooleanModel, []) as BooleanModel;
  }

  get fieldObject() {
    return this[getKeyModelSymbol]('fieldObject', ObjectModel, []) as ObjectModel<object>;
  }

  get fieldArrayString() {
    return this[getKeyModelSymbol]('fieldArrayString', ArrayModel, [StringModel, []]) as ArrayModel<string, StringModel>;
  }

  get fieldArrayModel() {
    return this[getKeyModelSymbol]('fieldArrayModel', ArrayModel, [IdEntityModel, []]) as ArrayModel<IdEntity, IdEntityModel>;
  }

  get fieldMatrixNumber() {
    return this[getKeyModelSymbol]('fieldMatrixNumber', ArrayModel, [ArrayModel, [NumberModel, [new Positive()]]]) as ArrayModel<ReadonlyArray<number>, ArrayModel<number, NumberModel>>;
  }
}
