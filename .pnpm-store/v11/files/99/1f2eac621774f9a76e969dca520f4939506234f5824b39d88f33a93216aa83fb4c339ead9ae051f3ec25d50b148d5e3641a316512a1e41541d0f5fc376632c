"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const model_1 = require("../model/model");
const polymer_element_1 = require("./polymer-element");
class ScannedPolymerElementMixin extends model_1.ScannedElementMixin {
    constructor({ name, jsdoc, description, summary, privacy, sourceRange, mixins, astNode, statementAst, classAstNode }) {
        super({ name });
        this.properties = new Map();
        this.methods = new Map();
        this.staticMethods = new Map();
        this.observers = [];
        this.listeners = [];
        this.behaviorAssignments = [];
        this.pseudo = false;
        this.abstract = false;
        this.jsdoc = jsdoc;
        this.description = description;
        this.summary = summary;
        this.privacy = privacy;
        this.sourceRange = sourceRange;
        this.mixins = mixins;
        this.astNode = astNode;
        this.statementAst = statementAst;
        this.classAstNode = classAstNode;
    }
    addProperty(prop) {
        polymer_element_1.addProperty(this, prop);
    }
    addMethod(method) {
        polymer_element_1.addMethod(this, method);
    }
    resolve(document) {
        return new PolymerElementMixin(this, document);
    }
}
exports.ScannedPolymerElementMixin = ScannedPolymerElementMixin;
class PolymerElementMixin extends model_1.ElementMixin {
    constructor(scannedMixin, document) {
        super(scannedMixin, document);
        this.listeners = [];
        this.behaviorAssignments = [];
        this.localIds = [];
        this.kinds.add('polymer-element-mixin');
        this.pseudo = scannedMixin.pseudo;
        this.behaviorAssignments = Array.from(scannedMixin.behaviorAssignments);
        this.observers = Array.from(scannedMixin.observers);
    }
    emitPropertyMetadata(property) {
        return {
            polymer: {
                notify: property.notify,
                observer: property.observer,
                readOnly: property.readOnly,
                attributeType: property.attributeType,
            }
        };
    }
    _getSuperclassAndMixins(document, init) {
        const prototypeChain = super._getSuperclassAndMixins(document, init);
        const { warnings, behaviors } = polymer_element_1.getBehaviors(init.behaviorAssignments, document);
        this.warnings.push(...warnings);
        prototypeChain.push(...behaviors);
        return prototypeChain;
    }
}
exports.PolymerElementMixin = PolymerElementMixin;
//# sourceMappingURL=polymer-element-mixin.js.map