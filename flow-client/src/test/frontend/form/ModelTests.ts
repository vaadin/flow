/* tslint:disable:max-classes-per-file */

const {suite, test, beforeEach, afterEach} = intern.getInterface("tdd");
const {assert} = intern.getPlugin("chai");
/// <reference types="sinon">
const {sinon} = intern.getPlugin('sinon');
import { expect } from "chai";

// API to test
import {
  Binder,
  getName,
  getValue,
  keySymbol,
  prependItem,
  setValue,
  BinderConfiguration,
  StringModel
} from "../../../main/resources/META-INF/resources/frontend/form";

import { Order, OrderModel, ProductModel } from "./TestModels";

suite("form/Model", () => {

  test("NotEmpty validator should indicate Required", () => {
    
  });

});
