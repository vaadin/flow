/* tslint:disable:max-classes-per-file */
// API to test
import {
  ArrayModel,
  BooleanModel,
  NumberModel,
  ObjectModel,
  Pattern,
  Positive,
  Required, Size, StringModel
} from "../../main/resources/META-INF/resources/frontend/form";

export interface IdEntity {
  idString: string;
}
export class IdEntityModel<T extends IdEntity = IdEntity> extends ObjectModel<T> {
  static createEmptyValue: () => IdEntity;
  readonly idString = new StringModel(this, 'idString');
}

export interface Product extends IdEntity {
  description: string;
  price: number;
  isInStock: boolean;
}
export class ProductModel<T extends Product = Product> extends IdEntityModel<T> {
  static createEmptyValue: () => Product;
  readonly description = new StringModel(this, 'description', new Required());
  readonly price = new NumberModel(this, 'price', new Positive());
  readonly isInStock = new BooleanModel(this, 'isInStock');
}

interface Customer extends IdEntity {
  fullName: string;
  nickName: string;
}
export class CustomerModel<T extends Customer = Customer> extends IdEntityModel<T> {
  static createEmptyValue: () => Customer;
  readonly fullName = new StringModel(this, 'fullName', new Size({min: 4}), new Required());
  readonly nickName = new StringModel(this, 'nickName', new Pattern("....*"));
}

export interface Order extends IdEntity {
  customer: Customer;
  notes: string;
  priority: number;
  products: ReadonlyArray<Product>;
}
export class OrderModel<T extends Order = Order> extends IdEntityModel<T> {
  static createEmptyValue: () => Order;
  readonly customer = new CustomerModel(this, 'customer', new Required());
  readonly notes = new StringModel(this, 'notes', new Required());
  readonly priority = new NumberModel(this, 'priority');
  readonly products = new ArrayModel(this, 'products', ProductModel);
}

