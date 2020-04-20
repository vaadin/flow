const { suite, test, beforeEach, afterEach } = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import {AbstractModel,
    ObjectModel,
    ArrayModel,
    StringModel,
    NumberModel,
    BooleanModel} from "../../main/resources/META-INF/resources/frontend/Binder";

suite("Binder", () => {

  beforeEach(() => {
    
  });

  afterEach(() => {
    
  });

  test("should be able to import and use data models", () => {
      class IdEntityModel<T> extends ObjectModel<T> {
        readonly idString = new StringModel(this, 'idString');
      }
      
      class LocationModel<T> extends IdEntityModel<T> {
        readonly description = new StringModel(this, 'description');
      }
      
      class ProductModel<T> extends IdEntityModel<T> {
        readonly description = new StringModel(this, 'description');
        readonly price = new NumberModel(this, 'price');
        readonly quantity = new NumberModel(this, 'quantity');
      }
      
      class CustomerModel<T> extends IdEntityModel<T> {
        readonly fullName = new StringModel(this, 'fullName');
        readonly email = new StringModel(this, 'email');
        readonly phoneNumber = new StringModel(this, 'phoneNumber');
      }
      
      class OrderModel<T> extends IdEntityModel<T> {
        readonly customer = new CustomerModel(this, 'customer');
        readonly dueDate = new StringModel(this, 'dueDate');
        readonly dueTime = new StringModel(this, 'dueTime');
        readonly notes = new StringModel(this, 'notes');
        readonly pickupLocation = new LocationModel(this, 'pickupLocation');
        readonly products = new ArrayModel<any, ProductModel<any>>(this, 'products', ProductModel);
      }  

  });

});

