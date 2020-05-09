/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');
import { expect } from "chai";

// API to test
import {
  Binder,
  getModelValidators,
  getName,
  getValue,
  setValue,
  validate,
  Validator,
  modelRepeat,
  field,
  appendItem,
  keySymbol,
  prependItem
} from "../../main/resources/META-INF/resources/frontend/Binder";

import { Order, OrderModel, ProductModel } from "./BinderModels";

import { customElement, html, LitElement, query, css} from 'lit-element';

@customElement('lit-order-view')
class LitOrderView extends LitElement {}

@customElement('order-view')
export default class OrderView extends LitElement {
  public binder = new Binder(this, OrderModel, () => this.requestUpdate());
  @query('#notes') public notes!: HTMLInputElement;
  @query('#fullName') public fullName!: HTMLInputElement;
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

suite("Binder", () => {
  const litOrderView = document.createElement('lit-order-view') as LitOrderView;
  const requestUpdateStub = sinon.stub(litOrderView, 'requestUpdate').resolves();

  afterEach(() => {
    requestUpdateStub.reset();
  });

  test("should instantiate without type arguments", () => {
    const binder = new Binder(litOrderView, OrderModel, () => litOrderView.requestUpdate());

    assert.isDefined(binder);
    assert.isDefined(binder.value.notes);
    assert.isDefined(binder.value.idString);
    assert.isDefined(binder.value.customer.fullName);
    assert.isDefined(binder.value.customer.idString);
  });

  test("should instantiate model", () => {
    const binder = new Binder(litOrderView, OrderModel, () => litOrderView.requestUpdate());

    assert.instanceOf(binder.model, OrderModel);
  });

  suite("name value", () => {
    let binder: Binder<Order, OrderModel<Order>>;

    const expectedEmptyOrder: Order = {
      idString: '',
      customer: {
        idString: '',
        fullName: '',
      },
      notes: '',
      priority: 0,
      products: []
    };

    beforeEach(() => {
      binder = new Binder(
        litOrderView,
        OrderModel,
        () => litOrderView.requestUpdate()
      );
      requestUpdateStub.reset();
    });

    test("should have name for models", () => {
      assert.equal(getName(binder.model.notes), "notes");
      assert.equal(getName(binder.model.customer.fullName), "customer.fullName");
    });

    test("should have initial defaultValue", () => {
      assert.deepEqual(binder.defaultValue, expectedEmptyOrder);
    });

    test("should have initial value", () => {
      assert.equal(binder.value, binder.defaultValue);
      assert.equal(getValue(binder.model), binder.value);
      assert.equal(getValue(binder.model.notes), "");
      assert.equal(getValue(binder.model.customer.fullName), "");
    });

    test("should change value on setValue", () => {
      // Sanity check: requestUpdate should not be called
      sinon.assert.notCalled(requestUpdateStub);

      setValue(binder.model.notes, "foo");
      assert.equal(binder.value.notes, "foo");
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should change value on deep setValue", () => {
      sinon.assert.notCalled(requestUpdateStub);

      setValue(binder.model.customer.fullName, "foo");
      assert.equal(binder.value.customer.fullName, "foo");
      sinon.assert.calledOnce(litOrderView.requestUpdate);
    });

    test("should not change defaultValue on setValue", () => {
      setValue(binder.model.notes, "foo");
      setValue(binder.model.customer.fullName, "foo");

      assert.equal(binder.defaultValue.notes, "");
      assert.equal(binder.defaultValue.customer.fullName, "");
    });

    test("should reset to default value", () => {
      setValue(binder.model.notes, "foo");
      setValue(binder.model.customer.fullName, "foo");
      requestUpdateStub.reset();

      binder.reset();

      assert.equal(binder.value.notes, "");
      assert.equal(binder.value.customer.fullName, "");
      sinon.assert.calledOnce(requestUpdateStub);
    });

    test("should reset to provided value", () => {
      setValue(binder.model.notes, "foo");
      setValue(binder.model.customer.fullName, "foo");
      requestUpdateStub.reset();

      binder.reset({
        ...expectedEmptyOrder,
        notes: "bar",
        customer: {
          ...expectedEmptyOrder.customer,
          fullName: "bar"
        }
      });

      assert.equal(binder.value.notes, "bar");
      assert.equal(binder.value.customer.fullName, "bar");
      sinon.assert.calledOnce(requestUpdateStub);
    });
  });

  suite('array-model', () => {
    let binder: Binder<Order, OrderModel<Order>>;

    beforeEach(() => {
      binder = new Binder(litOrderView, OrderModel, () => { });
    });

    test("should reuse model instance for the same array item", async () => {
      const products = [
        ProductModel.createEmptyValue(),
        ProductModel.createEmptyValue()
      ]
      setValue(binder.model.products, products.slice());
      const models_1 = [...binder.model.products].slice();
      [0, 1].forEach(i => expect(models_1[i].valueOf()).to.be.equal(products[i]));

      setValue(binder.model.products, products);
      const models_2 = [...binder.model.products].slice();
      [0, 1].forEach(i => {
        expect(models_1[i]).to.be.equal(models_2[i]);
        expect(models_2[i].valueOf()).to.be.equal(products[i]);
      });
    });

    test("should reuse model instance for the same array item after it is modified", async () => {
      const products = [
        ProductModel.createEmptyValue(),
        ProductModel.createEmptyValue()
      ]
      setValue(binder.model.products, products);
      const models_1 = [...binder.model.products].slice();
      [0, 1].forEach(i => expect(models_1[i].valueOf()).to.be.equal(products[i]));

      setValue(models_1[0].description, 'foo');
      setValue(models_1[1].description, 'bar');

      setValue(binder.model.products, products.slice());
      const models_2 = [...binder.model.products].slice();
      [0, 1].forEach(i => {
        expect(models_1[i]).to.be.equal(models_2[i]);
        expect(models_2[i].valueOf()).to.be.equal(products[i]);
      });
    });

    test("should update model keySymbol when inserting items", async () => {
      const products = [
        ProductModel.createEmptyValue(),
        ProductModel.createEmptyValue()
      ]
      setValue(binder.model.products, products);

      const models_1 = [...binder.model.products].slice();
      for (let i = 0; i < models_1.length; i++) {
        expect((models_1[i] as any)[keySymbol]).to.be.equal(i)
      }

      setValue(models_1[0].description, 'foo');
      expect(binder.model.products.valueOf()[0].description).to.be.equal('foo');

      prependItem(binder.model.products);
      expect(binder.model.products.valueOf()[1].description).to.be.equal('foo');

      const models_2 = [...binder.model.products].slice();
      expect(models_2.length).to.be.equal(3);
      for (let i = 0; i < models_2.length; i++) {
        expect((models_2[i] as any)[keySymbol]).to.be.equal(i)
      }
    });
  });

  suite("validation", () => {
    let binder: Binder<Order, OrderModel<Order>>;

    beforeEach(async () => {
      binder = new Binder(
        litOrderView,
        OrderModel,
        () => litOrderView.requestUpdate()
      );
    });

    test("should not have validation errors for a model without validators", async () => {
      assert.isEmpty(await validate(binder.model.priority));
    });

    test("should fail validation after adding a synchronous validator", async () => {
      class SyncValidator implements Validator<Order>{
        message = "foo";
        validate = () => false;
      }
      getModelValidators(binder.model.priority).add(new SyncValidator());
      return validate(binder.model.priority).then(errMsg => {
        expect(errMsg[0].error).to.equal("foo");
      });
    });

    test("should fail validation after adding an asynchronous validator", () => {
      class AsyncValidator implements Validator<Order>{
        message = "bar";
        validate = async () =>{
          await new Promise(resolve => setTimeout(resolve, 10));
          return false;
        };
      }
      getModelValidators(binder.model.priority).add(new AsyncValidator());
      return validate(binder.model.priority).then(errMsg => {
        expect(errMsg[0].error).to.equal("bar");
      });
    });

    suite('field element', () => {
      let orderView: OrderView;

      beforeEach(async () => {
        orderView = document.createElement('order-view') as OrderView;
        binder = new Binder(orderView, OrderModel, () => orderView.requestUpdate());
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

        try {
          await orderView.binder.submitTo(async (item) => item);
          expect.fail();
        } catch (error) {
        }

        expect(orderView.notes.hasAttribute('invalid')).to.be.true;
        expect(orderView.fullName.hasAttribute('invalid')).to.be.true;
      });

      test(`should validate fields of nested model on submit`, async () => {
        expect(orderView.description).to.be.null;
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
});



