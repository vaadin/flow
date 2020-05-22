/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');
import { expect } from "chai";

// API to test
import {
  appendItem,
  Binder,
  field,
  getModelValidators,
  modelRepeat,
  Required,
  setValue,
  validate,
  ValidationError,
  Validator
} from "../../../main/resources/META-INF/resources/frontend/form";

import { IdEntity, IdEntityModel,  Order, OrderModel } from "./TestModels";

import { css, customElement, html, LitElement, query} from 'lit-element';

@customElement('order-view')
class OrderView extends LitElement {
  public binder = new Binder(this, OrderModel);
  @query('#notes') public notes!: HTMLInputElement;
  @query('#fullName') public fullName!: HTMLInputElement;
  @query('#nickName') public nickName!: HTMLInputElement;
  @query('#add') public add!: Element;
  @query('#description0') public description!: HTMLInputElement;
  @query('#price0') public price!: HTMLInputElement;

  static get styles() {
    return css`input[invalid] {border: 2px solid red;}`;
  }
  render() {
    return html`
    <input id="notes" ...="${field(this.binder.model.notes)}" />
    <input id="fullName" ...="${field(this.binder.model.customer.fullName)}" />
    <input id="nickName" ...="${field(this.binder.model.customer.nickName)}" />
    <button id="add" @click=${() => appendItem(this.binder.model.products)}>+</button>
    ${modelRepeat(this.binder.model.products, (model, _product, index) => html`<div>
        <input id="description${index}" ...="${field(model.description)}" />
        <input id="price${index}" ...="${field(model.price)}">
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
  let view = document.createElement('div');

  beforeEach(async () => {
    binder = new Binder(view,OrderModel);
  });

  test("should run all validators per model", async () => {
    return validate(binder.model.customer).then(errors => {
      expect(errors.map(e => e.validator.constructor.name).sort()).to.eql([
        "Required",
        "Size"
      ]);
    });
  });

  test("should run all nested validations per model", async () => {
    return validate(binder.model).then(errors => {
      expect(errors.map(e => e.property)).to.eql([
        "customer.fullName",
        "customer.fullName",
        "notes"
      ]);
    });
  });

  test("should run all validations per array items", async () => {
    appendItem(binder.model.products);
    appendItem(binder.model.products);
    return validate(binder.model).then(errors => {
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
      let foo = 'bar';
      const binder = new Binder(view, OrderModel, {
        onSubmit: async () => {
          foo = 'baz';
        }
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
        expect(error.errors[0].validator.message).to.be.equal('custom message');
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
        expect(error.errors[0].validator.message).to.be.equal('Custom server message');
        expect(error.errors[0].value).to.be.undefined;
        expect(error.errors[0].property).to.be.equal('bar');
      }
    });
  });

  suite('model add validator', () => {
    let binder: Binder<IdEntity, IdEntityModel<IdEntity>>;

    beforeEach(async () => {
      binder = new Binder(view, IdEntityModel);
    });

    test("should not have validation errors for a model without validators", async () => {
      assert.isEmpty(await validate(binder.model));
    });

    test("should fail validation after adding a synchronous validator to the model", async () => {
      getModelValidators(binder.model).add({message: 'foo', validate: () => false});
      return validate(binder.model).then(errors => {
        expect(errors[0].validator.message).to.equal("foo");
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
      getModelValidators(binder.model).add(new AsyncValidator());
      return validate(binder.model).then(errors => {
        expect(errors[0].validator.message).to.equal("bar");
      });
    });

    test("should not have validations errors after adding validators to properties if property is not required", async () => {
      getModelValidators(binder.model.idString).add({message: 'foo', validate: () => false});
      const errors = await validate(binder.model);
      assert.isEmpty(errors);
    });

    test("should fail after adding validators to properties if property is not required but it has a value", async () => {
      setValue(binder.model.idString, 'bar');
      getModelValidators(binder.model.idString).add({message: 'foo', validate: () => false});
      const errors = await validate(binder.model);
      expect(errors[0].validator.message).to.equal("foo");
      expect(errors[0].property).to.equal('idString');
      expect(errors[0].value).to.eql('bar');
    });

    test("should fail after adding validators to properties if required and not value", async () => {
      getModelValidators(binder.model.idString).add({message: 'foo', validate: () => false});
      getModelValidators(binder.model.idString).add(new Required());
      const errors = await validate(binder.model);
      expect(errors.length).to.equal(2);
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
