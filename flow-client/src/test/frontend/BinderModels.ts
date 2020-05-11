/* tslint:disable:max-classes-per-file */
// API to test
import {
  ArrayModel,
  BooleanModel,
  NumberModel,
  ObjectModel,
  StringModel,
  Required
} from "../../main/resources/META-INF/resources/frontend/Binder";

import {
   Positive, Size
} from "../../main/resources/META-INF/resources/frontend/FormValidator";

interface IdEntity {
  idString: string;
}
class IdEntityModel<T extends IdEntity = IdEntity> extends ObjectModel<T> {
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
}
export class CustomerModel<T extends Customer = Customer> extends IdEntityModel<T> {
  static createEmptyValue: () => Customer;
  readonly fullName = new StringModel(this, 'fullName', new Required(), new Size({min: 4}));
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

