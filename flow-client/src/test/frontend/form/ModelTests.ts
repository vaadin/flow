/* tslint:disable: no-unused-expression */

const {suite, test} = intern.getInterface("tdd");
import { expect } from "chai";

// API to test
import {
  Binder,
  getModelValidators,
  NotEmpty,
  requiredSymbol,
  Required,
  NotNull,
  Size} from "../../../main/resources/META-INF/resources/frontend/form";

import { TestModel } from "./TestModels";

suite("form/Model", () => {

  suite('model/requiredFlag', () => {

    test(`NotEmpty validator should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.model.fieldString[requiredSymbol]).to.be.false;
      getModelValidators(binder.model.fieldString).add(new NotEmpty());
      expect(binder.model.fieldString[requiredSymbol]).to.be.true;
    });

    test(`NotNull validator should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.model.fieldString[requiredSymbol]).to.be.false;
      getModelValidators(binder.model.fieldString).add(new NotNull());
      expect(binder.model.fieldString[requiredSymbol]).to.be.true;
    });

    test(`NotBlank validator should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.model.fieldString[requiredSymbol]).to.be.false;
      getModelValidators(binder.model.fieldString).add(new NotNull());
      expect(binder.model.fieldString[requiredSymbol]).to.be.true;
    });

    test(`Size validator with min bigger than 0 should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.model.fieldString[requiredSymbol]).to.be.false;
      getModelValidators(binder.model.fieldString).add(new Size({min:1}));
      expect(binder.model.fieldString[requiredSymbol]).to.be.true;
    });

    test(`Size validator with min 0 should not be mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.model.fieldString[requiredSymbol]).to.be.false;
      getModelValidators(binder.model.fieldString).add(new Size({min:0}));
      expect(binder.model.fieldString[requiredSymbol]).to.be.false;
    });
  })
});
