/* tslint:disable: no-unused-expression */

const {suite, test} = intern.getInterface("tdd");
import { expect } from "chai";

// API to test
import {
  Binder,
  NotEmpty,
  Required,
  NotNull,
  Size,
  NotBlank} from "../../../main/resources/META-INF/resources/frontend/form";

import { TestModel } from "./TestModels";

suite("form/Model", () => {

  suite('model/requiredFlag', () => {

    test(`NotEmpty validator should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.for(binder.model.fieldString).required).to.be.false;
      binder.for(binder.model.fieldString).addValidator(new NotEmpty());
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`NotNull validator should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.for(binder.model.fieldString).required).to.be.false;
      binder.for(binder.model.fieldString).addValidator(new NotNull());
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`NotBlank validator should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.for(binder.model.fieldString).required).to.be.false;
      binder.for(binder.model.fieldString).addValidator(new NotBlank());
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`Size validator with min bigger than 0 should mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.for(binder.model.fieldString).required).to.be.false;
      binder.for(binder.model.fieldString).addValidator(new Size({min:1}));
      expect(binder.for(binder.model.fieldString).required).to.be.true;
    });

    test(`Size validator with min 0 should not be mark a model as required`, async () => {
      const binder = new Binder(document.createElement('div'), TestModel);
      expect(binder.for(binder.model.fieldString).required).to.be.false;
      binder.for(binder.model.fieldString).addValidator(new Size({min:0}));
      expect(binder.for(binder.model.fieldString).required).to.be.false;
    });
  })
});
