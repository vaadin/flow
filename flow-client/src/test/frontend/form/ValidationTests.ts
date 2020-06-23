/* tslint:disable:max-classes-per-file */

import {repeat} from "lit-html/directives/repeat";

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');
import { expect } from "chai";

// API to test
import {
  Binder,
  field,
  getName,
  Required,
  setValue,
  ValidationError,
  Validator,
  ValueError
} from "../../../main/resources/META-INF/resources/frontend/form";

import { IdEntity, IdEntityModel,  Order, OrderModel, TestEntity, TestModel } from "./TestModels";

import { css, customElement, html, LitElement, query} from 'lit-element';

@customElement('order-view')
class OrderView extends LitElement {
  binder = new Binder(this, OrderModel);
  @query('#notes') notes!: HTMLInputElement;
  @query('#fullName') fullName!: HTMLInputElement;
  @query('#nickName') nickName!: HTMLInputElement;
  @query('#add') add!: Element;
  @query('#description0') description!: HTMLInputElement;
  @query('#price0') price!: HTMLInputElement;

  static get styles() {
    return css`input[invalid] {border: 2px solid red;}`;
  }
  render() {
    const {notes, products, customer: {fullName, nickName}} = this.binder.model;
    return html`
    <input id="notes" ...="${field(notes)}" />
    <input id="fullName" ...="${field(fullName)}" />
    <input id="nickName" ...="${field(nickName)}" />
    <button id="add" @click=${() => this.binder.for(products).appendItem()}>+</button>
    ${repeat(products, ({model: {description, price}}, index) => html`<div>
        <input id="description${index}" ...="${field(description)}" />
        <input id="price${index}" ...="${field(price)}">
      </div>`)}
    `;
  }
}

const sleep = async (t: number) => new Promise(resolve => setTimeout(() => resolve(), t));
const fireEvent = async (elm: Element, name: string) => {
  elm.dispatchEvent(new CustomEvent(name));
  return sleep(0);
}

suite("form/Validation", () => {
  let binder: Binder<Order, OrderModel<Order>>;
  const view = document.createElement('div');

  beforeEach(async () => {
    binder = new Binder(view, OrderModel);
  });

  test("should run all validators per model", async () => {
    return binder.for(binder.model.customer).validate().then(errors => {
      expect(errors.map(e => e.validator.constructor.name).sort()).to.eql([
        "Required",
        "Size"
      ]);
    });
  });

  test("should run all nested validations per model", async () => {
    return binder.validate().then(errors => {
      expect(errors.map(e => e.property)).to.eql([
        "customer.fullName",
        "customer.fullName",
        "notes"
      ]);
    });
  });

  test("should run all validations per array items", async () => {
    binder.for(binder.model.products).appendItem();
    binder.for(binder.model.products).appendItem();
    return binder.validate().then(errors => {
      expect(errors.map(e => e.property)).to.eql([
        "customer.fullName",
        "customer.fullName",
        "notes",
        "products.0.description",
        "products.0.price",
        "products.1.description",
        "products.1.price"
      ]);
    });
  });

  suite('submitTo', () => {
    test("should be able to call submit() if onSubmit is pre configured", async () => {
      const binder = new Binder(view, OrderModel, {
        onSubmit: async () => {}
      });
      const binderSubmitToSpy = sinon.spy(binder, 'submitTo');
      await binder.submit();
      sinon.assert.calledOnce(binderSubmitToSpy);
    });

    test("should throw on validation failure", async () => {
      try {
        await binder.submitTo(async() => {});
        expect.fail();
      } catch (error) {
        expect(error.errors.length).to.gt(0);
      }
    });

    test("should re-throw on server failure", async () => {
      setValue(binder.model.customer.fullName, 'foobar');
      setValue(binder.model.notes, 'whatever');
      try {
        await binder.submitTo(async() => {throw new Error('whatever')});
        expect.fail();
      } catch (error) {
        expect(error.message).to.be.equal('whatever');
      }
    });

    test("should wrap server validation error", async () => {
      setValue(binder.model.customer.fullName, 'foobar');
      setValue(binder.model.notes, 'whatever');
      try {
        await binder.submitTo(async() => {throw {
          message: "Validation error in endpoint 'MyEndpoint' method 'saveMyBean'",
          validationErrorData: [{
            message: "Object of type 'com.example.MyBean' has invalid property 'foo' with value 'baz', validation error: 'custom message'",
            parameterName: "foo",
          }]
        }});
        expect.fail();
      } catch (error) {
        expect(error.errors[0].message).to.be.equal('custom message');
        expect(error.errors[0].value).to.be.equal('baz');
        expect(error.errors[0].property).to.be.equal('foo');
      }
    });

    test("should wrap server validation error with any message", async () => {
      setValue(binder.model.customer.fullName, 'foobar');
      setValue(binder.model.notes, 'whatever');
      try {
        await binder.submitTo(async() => {throw {
          message: "Validation error in endpoint 'MyEndpoint' method 'saveMyBean'",
          validationErrorData: [{
            message: "Custom server message",
            parameterName: "bar",
          }]
        }});
        expect.fail();
      } catch (error) {
        expect(error.errors[0].message).to.be.equal('Custom server message');
        expect(error.errors[0].value).to.be.undefined;
        expect(error.errors[0].property).to.be.equal('bar');
      }
    });

    test("record level cross field validation", async () => {
      const byPropertyName = (value: string) => ((error: ValueError<any>) => {
        const propertyName = typeof error.property === 'string' ? error.property : getName(error.property);
        return propertyName === value;
      });

      const recordValidator = {
        validate(value: Order) {
          if (value.customer.fullName === value.customer.nickName) {
            return { property: binder.model.customer.nickName };
          }

          return true;
        },
        message: 'cannot be the same'
      };
      binder.addValidator(recordValidator);

      setValue(binder.model.customer.fullName, "foo");
      await binder.validate().then(errors => {
        const crossFieldError = errors.find(error => error.validator === recordValidator);
        expect(crossFieldError, 'recordValidator should not cause an error').to.be.undefined;
      });

      setValue(binder.model.customer.nickName, "foo");
      return binder.validate().then(errors => {
        const crossFieldError = errors.find(byPropertyName('customer.nickName'));
        expect(crossFieldError).not.to.be.undefined;
        crossFieldError && expect(crossFieldError.message).to.equal('cannot be the same');
      });
    });
  });

  suite('model add validator', () => {
    let binder: Binder<IdEntity, IdEntityModel<IdEntity>>;

    beforeEach(async () => {
      binder = new Binder(view, IdEntityModel);
    });

    test("should not have validation errors for a model without validators", async () => {
      assert.isEmpty(await binder.validate());
    });

    test("should not have validation errors for a validator that returns true", async () => {
      binder.addValidator({message: 'foo', validate: () => true});
      assert.isEmpty(await binder.validate());
    });

    test("should not have validation errors for a validator that returns an empty array", async () => {
      binder.addValidator({message: 'foo', validate: () => []});
      assert.isEmpty(await binder.validate());
    });

    test("should fail validation after adding a synchronous validator to the model", async () => {
      binder.addValidator({message: 'foo', validate: () => false});
      return binder.validate().then(errors => {
        expect(errors[0].message).to.equal("foo");
        expect(errors[0].property).to.equal('');
        expect(errors[0].value).to.eql({idString: ''});
      });
    });

    test("should fail validation after adding an asynchronous validator to the model", async () => {
      class AsyncValidator implements Validator<Order>{
        message = "bar";
        validate = async () => {
          await sleep(10);
          return false;
        };
      }
      binder.addValidator(new AsyncValidator());
      return binder.validate().then(errors => {
        expect(errors[0].message).to.equal("bar");
      });
    });

    test("should not have validations errors after adding validators to properties if property is not required", async () => {
      binder.for(binder.model.idString).addValidator({message: 'foo', validate: () => false});
      const errors = await binder.validate();
      assert.isEmpty(errors);
    });

    test("should fail after adding validators to properties if property is not required but it has a value", async () => {
      setValue(binder.model.idString, 'bar');
      binder.for(binder.model.idString).addValidator({message: 'foo', validate: () => false});
      const errors = await binder.validate();
      expect(errors[0].message).to.equal("foo");
      expect(errors[0].property).to.equal('idString');
      expect(errors[0].value).to.eql('bar');
    });

    test("should fail after adding validators to properties if required and not value", async () => {
      binder.for(binder.model.idString).addValidator({message: 'foo', validate: () => false});
      binder.for(binder.model.idString).addValidator(new Required());
      const errors = await binder.validate();
      expect(errors.length).to.equal(2);
    });

    test("should fail when validator returns a single ValidationResult", async () => {
      binder.addValidator({message: 'foo', validate: () => ({ property: binder.model.idString })});
      return binder.validate().then(errors => {
        expect(errors[0].message).to.equal('foo');
        expect(errors[0].property).to.equal(binder.model.idString);
        expect(errors[0].value).to.eql({idString: ''});
      });
    });

    test("should fail when validator returns an array of ValidationResult objects", async () => {
      binder.addValidator({message: 'foo', validate: () => [{ property: binder.model.idString }]});
      return binder.validate().then(errors => {
        expect(errors[0].message).to.equal('foo');
        expect(errors[0].property).to.equal(binder.model.idString);
        expect(errors[0].value).to.eql({idString: ''});
      });
    });
  });

  suite('model add validator (multiple fields)', () => {
    let binder: Binder<TestEntity, TestModel<TestEntity>>;

    beforeEach(async () => {
      binder = new Binder(view, TestModel);
    });

    test("should fail when validator returns an array of ValidationResult objects", async () => {
      binder.addValidator({message: 'foo', validate: () => [
          { property: binder.model.fieldString },
          { property: binder.model.fieldNumber },
          { property: binder.model.fieldBoolean, message: 'bar' }
          ]});
      return binder.validate().then(errors => {
        expect(errors).has.lengthOf(3);
        expect(errors[0].message).to.equal('foo');
        expect(errors[0].value).to.eql(TestModel.createEmptyValue());

        expect(errors[0].property).to.equal(binder.model.fieldString);
        expect(errors[1].property).to.equal(binder.model.fieldNumber);
        expect(errors[2].property).to.equal(binder.model.fieldBoolean);
        expect(errors[2].message).to.equal('bar');
      });
    });
  });

  suite('field element', () => {
    let orderView: OrderView;

    beforeEach(async () => {
      orderView = document.createElement('order-view') as OrderView;
      binder = new Binder(orderView, OrderModel);
      document.body.appendChild(orderView);
      return sleep(10);
    });

    afterEach(async () => {
      document.body.removeChild(orderView)
    });

    ['change', 'blur'].forEach(event => {
      test(`should validate field on ${event}`, async () => {
        expect(orderView.notes.hasAttribute('invalid')).to.be.false;
        await fireEvent(orderView.notes, event);
        expect(orderView.notes.hasAttribute('invalid')).to.be.true;
      });

      test(`should validate field of nested model on  ${event}`, async () => {
        await fireEvent(orderView.add, 'click');
        expect(orderView.description.hasAttribute('invalid')).to.be.false;
        await fireEvent(orderView.description, event);
        expect(orderView.description.hasAttribute('invalid')).to.be.true;
      });
    });

    test(`should not validate field on input when first visit`, async () => {
      expect(orderView.notes.hasAttribute('invalid')).to.be.false;
      await fireEvent(orderView.notes, 'input');
      expect(orderView.notes.hasAttribute('invalid')).to.be.false;
    });

    test(`should validate field on input after first visit`, async () => {
      orderView.notes.value = 'foo';
      await fireEvent(orderView.notes, 'blur');
      expect(orderView.notes.hasAttribute('invalid')).to.be.false;

      orderView.notes.value = '';
      await fireEvent(orderView.notes, 'input');
      expect(orderView.notes.hasAttribute('invalid')).to.be.true;
    });

    test(`should validate fields on submit`, async () => {
      expect(orderView.notes.hasAttribute('invalid')).to.be.false;
      expect(orderView.fullName.hasAttribute('invalid')).to.be.false;
      expect(orderView.nickName.hasAttribute('invalid')).to.be.false;

      try {
        await orderView.binder.submitTo(async (item) => item);
        expect.fail();
      } catch (error) {
      }

      expect(orderView.notes.hasAttribute('invalid')).to.be.true;
      expect(orderView.fullName.hasAttribute('invalid')).to.be.true;
      expect(orderView.nickName.hasAttribute('invalid')).to.be.false;
    });

    test(`should validate fields of nested model on submit`, async () => {
      expect(orderView.description).to.be.null;
      await fireEvent(orderView.add, 'click');
      await fireEvent(orderView.add, 'click');

      expect(orderView.description.hasAttribute('invalid')).to.be.false;
      expect(orderView.price.hasAttribute('invalid')).to.be.false;

      try {
        await orderView.binder.submitTo(async (item) => item);
        expect.fail();
      } catch (error) {
        expect((error as ValidationError).errors.map(e => e.property)).to.be.eql([
          'customer.fullName',
          'customer.fullName',
          'notes',
          'products.0.description',
          'products.0.price',
          'products.1.description',
          'products.1.price'
        ]);
      }

      expect(orderView.description.hasAttribute('invalid')).to.be.true;
      expect(orderView.price.hasAttribute('invalid')).to.be.true;
    });

    test(`should validate fields of arrays on submit`, async () => {
      expect(orderView.description).to.be.null;
      await fireEvent(orderView.add, 'click');
      await fireEvent(orderView.add, 'click');

      expect(orderView.description.hasAttribute('invalid')).to.be.false;
      expect(orderView.price.hasAttribute('invalid')).to.be.false;

      try {
        await orderView.binder.submitTo(async (item) => item);
        expect.fail();
      } catch (error) {
      }

      expect(orderView.description.hasAttribute('invalid')).to.be.true;
      expect(orderView.price.hasAttribute('invalid')).to.be.true;
    });

    test(`should not submit when just validation fails`, async () => {
      expect(orderView.description).to.be.null;
      await fireEvent(orderView.add, 'click');

      orderView.notes.value = 'foo';
      await fireEvent(orderView.notes, 'change');
      orderView.fullName.value = 'manuel';
      await fireEvent(orderView.fullName, 'change');
      orderView.description.value = 'bread';
      await fireEvent(orderView.description, 'change');

      try {
        await orderView.binder.submitTo(async (item) => item);
        expect.fail();
      } catch (error) {
      }
    });

    test(`should submit when no validation errors`, async () => {
      expect(orderView.description).to.be.null;
      await fireEvent(orderView.add, 'click');

      orderView.notes.value = 'foo';
      await fireEvent(orderView.notes, 'change');
      orderView.fullName.value = 'manuel';
      await fireEvent(orderView.fullName, 'change');
      orderView.description.value = 'bread';
      await fireEvent(orderView.description, 'change');
      orderView.price.value = '10';
      await fireEvent(orderView.price, 'change');

      const item = await orderView.binder.submitTo(async (item) => item) as Order;
      expect(item).not.to.be.undefined;
      expect(item.products[0].description).to.be.equal('bread');
      expect(item.products[0].price).to.be.equal(10);
      expect(item.notes).to.be.equal('foo');
      expect(item.customer.fullName).to.be.equal('manuel');
    });
  });

});
