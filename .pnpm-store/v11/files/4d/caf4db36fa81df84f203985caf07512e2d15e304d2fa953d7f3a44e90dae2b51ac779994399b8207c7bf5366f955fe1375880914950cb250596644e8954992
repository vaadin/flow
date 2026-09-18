// Babel 7 doesn't have typings yet. These are minimal and temporary

declare module '@babel/core';

declare module '@babel/plugin-syntax-import-meta';

declare module '@babel/plugin-syntax-dynamic-import';

declare module '@babel/helper-plugin-utils';

declare module '@babel/template';

declare module '@babel/traverse' {
  import * as t from 'babel-types';
  export type Node = t.Node;

  export class Scope {
    constructor(path: NodePath, parentScope?: Scope);
    path: NodePath;
    block: Node;
    parentBlock: Node;
    parent: Scope;
    // hub: Hub;
    bindings: {[name: string]: Binding;};

    /** Generate a unique identifier and add it to the current scope. */
    generateDeclaredUidIdentifier(name?: string): t.Identifier;

    /** Generate a unique identifier. */
    generateUidIdentifier(name?: string): t.Identifier;

    /** Generate a unique `_id1` binding. */
    generateUid(name?: string): string;

    /** Walks the scope tree and gathers **all** bindings. */
    getAllBindings(...kinds: string[]): object;
  }

  export class Binding {
    constructor(opts: {
      existing: Binding; identifier: t.Identifier; scope: Scope; path: NodePath;
      kind: 'var' | 'let' | 'const';
    });
    identifier: t.Identifier;
    scope: Scope;
    path: NodePath;
    kind: 'var'|'let'|'const'|'module';
    referenced: boolean;
    references: number;
    referencePaths: NodePath[];
    constant: boolean;
    constantViolations: NodePath[];
  }

  export class NodePath<T = Node> {
    node: T;
    scope: Scope;

    traverse(visitor: Visitor, state?: any): void;

    // ------------------------- replacement -------------------------
    /**
     * Replace a node with an array of multiple. This method performs the
     * following steps:
     *
     *  - Inherit the comments of first provided node with that of the current
     * node.
     *  - Insert the provided nodes after the current node.
     *  - Remove the current node.
     */
    replaceWithMultiple(nodes: Node[]): void;

    /**
     * Parse a string as an expression and replace the current node with the
     * result.
     *
     * NOTE: This is typically not a good idea to use. Building source strings
     * when transforming ASTs is an antipattern and SHOULD NOT be encouraged.
     * Even if it's easier to use, your transforms will be extremely brittle.
     */
    replaceWithSourceString(replacement: any): void;

    /** Replace the current node with another. */
    replaceWith(replacement: Node|NodePath): void;

    /**
     * This method takes an array of statements nodes and then explodes it
     * into expressions. This method retains completion records which is
     * extremely important to retain original semantics.
     */
    replaceExpressionWithStatements(nodes: Node[]): Node;

    replaceInline(nodes: Node|Node[]): void;
  }


  // The Visitor has to be generic because babel binds `this` for each property.
  // `this` is usually used in babel plugins to pass plugin state from
  // `pre` -> `visitor` -> `post`. An example of this can be seen in the
  // official babel handbook:
  // https://github.com/thejameskyle/babel-handbook/blob/master/translations/en/plugin-handbook.md#-pre-and-post-in-plugins
  export interface Visitor<S = Node> extends VisitNodeObject<Node> {
    ArrayExpression?: VisitNode<S, t.ArrayExpression>;
    AssignmentExpression?: VisitNode<S, t.AssignmentExpression>;
    LVal?: VisitNode<S, t.LVal>;
    Expression?: VisitNode<S, t.Expression>;
    BinaryExpression?: VisitNode<S, t.BinaryExpression>;
    Directive?: VisitNode<S, t.Directive>;
    DirectiveLiteral?: VisitNode<S, t.DirectiveLiteral>;
    BlockStatement?: VisitNode<S, t.BlockStatement>;
    BreakStatement?: VisitNode<S, t.BreakStatement>;
    Identifier?: VisitNode<S, t.Identifier>;
    CallExpression?: VisitNode<S, t.CallExpression>;
    CatchClause?: VisitNode<S, t.CatchClause>;
    ConditionalExpression?: VisitNode<S, t.ConditionalExpression>;
    ContinueStatement?: VisitNode<S, t.ContinueStatement>;
    DebuggerStatement?: VisitNode<S, t.DebuggerStatement>;
    DoWhileStatement?: VisitNode<S, t.DoWhileStatement>;
    Statement?: VisitNode<S, t.Statement>;
    EmptyStatement?: VisitNode<S, t.EmptyStatement>;
    ExpressionStatement?: VisitNode<S, t.ExpressionStatement>;
    File?: VisitNode<S, t.File>;
    Program?: VisitNode<S, t.Program>;
    ForInStatement?: VisitNode<S, t.ForInStatement>;
    VariableDeclaration?: VisitNode<S, t.VariableDeclaration>;
    ForStatement?: VisitNode<S, t.ForStatement>;
    FunctionDeclaration?: VisitNode<S, t.FunctionDeclaration>;
    FunctionExpression?: VisitNode<S, t.FunctionExpression>;
    IfStatement?: VisitNode<S, t.IfStatement>;
    LabeledStatement?: VisitNode<S, t.LabeledStatement>;
    StringLiteral?: VisitNode<S, t.StringLiteral>;
    NumericLiteral?: VisitNode<S, t.NumericLiteral>;
    NullLiteral?: VisitNode<S, t.NullLiteral>;
    BooleanLiteral?: VisitNode<S, t.BooleanLiteral>;
    RegExpLiteral?: VisitNode<S, t.RegExpLiteral>;
    LogicalExpression?: VisitNode<S, t.LogicalExpression>;
    MemberExpression?: VisitNode<S, t.MemberExpression>;
    NewExpression?: VisitNode<S, t.NewExpression>;
    ObjectExpression?: VisitNode<S, t.ObjectExpression>;
    ObjectMethod?: VisitNode<S, t.ObjectMethod>;
    ObjectProperty?: VisitNode<S, t.ObjectProperty>;
    RestElement?: VisitNode<S, t.RestElement>;
    ReturnStatement?: VisitNode<S, t.ReturnStatement>;
    SequenceExpression?: VisitNode<S, t.SequenceExpression>;
    SwitchCase?: VisitNode<S, t.SwitchCase>;
    SwitchStatement?: VisitNode<S, t.SwitchStatement>;
    ThisExpression?: VisitNode<S, t.ThisExpression>;
    ThrowStatement?: VisitNode<S, t.ThrowStatement>;
    TryStatement?: VisitNode<S, t.TryStatement>;
    UnaryExpression?: VisitNode<S, t.UnaryExpression>;
    UpdateExpression?: VisitNode<S, t.UpdateExpression>;
    VariableDeclarator?: VisitNode<S, t.VariableDeclarator>;
    WhileStatement?: VisitNode<S, t.WhileStatement>;
    WithStatement?: VisitNode<S, t.WithStatement>;
    AssignmentPattern?: VisitNode<S, t.AssignmentPattern>;
    ArrayPattern?: VisitNode<S, t.ArrayPattern>;
    ArrowFunctionExpression?: VisitNode<S, t.ArrowFunctionExpression>;
    ClassBody?: VisitNode<S, t.ClassBody>;
    ClassDeclaration?: VisitNode<S, t.ClassDeclaration>;
    ClassExpression?: VisitNode<S, t.ClassExpression>;
    ExportAllDeclaration?: VisitNode<S, t.ExportAllDeclaration>;
    ExportDefaultDeclaration?: VisitNode<S, t.ExportDefaultDeclaration>;
    ExportNamedDeclaration?: VisitNode<S, t.ExportNamedDeclaration>;
    Declaration?: VisitNode<S, t.Declaration>;
    ExportSpecifier?: VisitNode<S, t.ExportSpecifier>;
    ForOfStatement?: VisitNode<S, t.ForOfStatement>;
    ImportDeclaration?: VisitNode<S, t.ImportDeclaration>;
    ImportDefaultSpecifier?: VisitNode<S, t.ImportDefaultSpecifier>;
    ImportNamespaceSpecifier?: VisitNode<S, t.ImportNamespaceSpecifier>;
    ImportSpecifier?: VisitNode<S, t.ImportSpecifier>;
    MetaProperty?: VisitNode<S, t.MetaProperty>;
    ClassMethod?: VisitNode<S, t.ClassMethod>;
    ObjectPattern?: VisitNode<S, t.ObjectPattern>;
    SpreadElement?: VisitNode<S, t.SpreadElement>;
    Super?: VisitNode<S, t.Super>;
    TaggedTemplateExpression?: VisitNode<S, t.TaggedTemplateExpression>;
    TemplateLiteral?: VisitNode<S, t.TemplateLiteral>;
    TemplateElement?: VisitNode<S, t.TemplateElement>;
    YieldExpression?: VisitNode<S, t.YieldExpression>;
    AnyTypeAnnotation?: VisitNode<S, t.AnyTypeAnnotation>;
    ArrayTypeAnnotation?: VisitNode<S, t.ArrayTypeAnnotation>;
    BooleanTypeAnnotation?: VisitNode<S, t.BooleanTypeAnnotation>;
    BooleanLiteralTypeAnnotation?: VisitNode<S, t.BooleanLiteralTypeAnnotation>;
    NullLiteralTypeAnnotation?: VisitNode<S, t.NullLiteralTypeAnnotation>;
    ClassImplements?: VisitNode<S, t.ClassImplements>;
    ClassProperty?: VisitNode<S, t.ClassProperty>;
    DeclareClass?: VisitNode<S, t.DeclareClass>;
    DeclareFunction?: VisitNode<S, t.DeclareFunction>;
    DeclareInterface?: VisitNode<S, t.DeclareInterface>;
    DeclareModule?: VisitNode<S, t.DeclareModule>;
    DeclareTypeAlias?: VisitNode<S, t.DeclareTypeAlias>;
    DeclareVariable?: VisitNode<S, t.DeclareVariable>;
    ExistentialTypeParam?: VisitNode<S, t.ExistentialTypeParam>;
    FunctionTypeAnnotation?: VisitNode<S, t.FunctionTypeAnnotation>;
    FunctionTypeParam?: VisitNode<S, t.FunctionTypeParam>;
    GenericTypeAnnotation?: VisitNode<S, t.GenericTypeAnnotation>;
    InterfaceExtends?: VisitNode<S, t.InterfaceExtends>;
    InterfaceDeclaration?: VisitNode<S, t.InterfaceDeclaration>;
    IntersectionTypeAnnotation?: VisitNode<S, t.IntersectionTypeAnnotation>;
    MixedTypeAnnotation?: VisitNode<S, t.MixedTypeAnnotation>;
    NullableTypeAnnotation?: VisitNode<S, t.NullableTypeAnnotation>;
    NumericLiteralTypeAnnotation?: VisitNode<S, t.NumericLiteralTypeAnnotation>;
    NumberTypeAnnotation?: VisitNode<S, t.NumberTypeAnnotation>;
    StringLiteralTypeAnnotation?: VisitNode<S, t.StringLiteralTypeAnnotation>;
    StringTypeAnnotation?: VisitNode<S, t.StringTypeAnnotation>;
    ThisTypeAnnotation?: VisitNode<S, t.ThisTypeAnnotation>;
    TupleTypeAnnotation?: VisitNode<S, t.TupleTypeAnnotation>;
    TypeofTypeAnnotation?: VisitNode<S, t.TypeofTypeAnnotation>;
    TypeAlias?: VisitNode<S, t.TypeAlias>;
    TypeAnnotation?: VisitNode<S, t.TypeAnnotation>;
    TypeCastExpression?: VisitNode<S, t.TypeCastExpression>;
    TypeParameterDeclaration?: VisitNode<S, t.TypeParameterDeclaration>;
    TypeParameterInstantiation?: VisitNode<S, t.TypeParameterInstantiation>;
    ObjectTypeAnnotation?: VisitNode<S, t.ObjectTypeAnnotation>;
    ObjectTypeCallProperty?: VisitNode<S, t.ObjectTypeCallProperty>;
    ObjectTypeIndexer?: VisitNode<S, t.ObjectTypeIndexer>;
    ObjectTypeProperty?: VisitNode<S, t.ObjectTypeProperty>;
    QualifiedTypeIdentifier?: VisitNode<S, t.QualifiedTypeIdentifier>;
    UnionTypeAnnotation?: VisitNode<S, t.UnionTypeAnnotation>;
    VoidTypeAnnotation?: VisitNode<S, t.VoidTypeAnnotation>;
    JSXAttribute?: VisitNode<S, t.JSXAttribute>;
    JSXIdentifier?: VisitNode<S, t.JSXIdentifier>;
    JSXNamespacedName?: VisitNode<S, t.JSXNamespacedName>;
    JSXElement?: VisitNode<S, t.JSXElement>;
    JSXExpressionContainer?: VisitNode<S, t.JSXExpressionContainer>;
    JSXClosingElement?: VisitNode<S, t.JSXClosingElement>;
    JSXMemberExpression?: VisitNode<S, t.JSXMemberExpression>;
    JSXOpeningElement?: VisitNode<S, t.JSXOpeningElement>;
    JSXEmptyExpression?: VisitNode<S, t.JSXEmptyExpression>;
    JSXSpreadAttribute?: VisitNode<S, t.JSXSpreadAttribute>;
    JSXText?: VisitNode<S, t.JSXText>;
    Noop?: VisitNode<S, t.Noop>;
    ParenthesizedExpression?: VisitNode<S, t.ParenthesizedExpression>;
    AwaitExpression?: VisitNode<S, t.AwaitExpression>;
    BindExpression?: VisitNode<S, t.BindExpression>;
    Decorator?: VisitNode<S, t.Decorator>;
    DoExpression?: VisitNode<S, t.DoExpression>;
    ExportDefaultSpecifier?: VisitNode<S, t.ExportDefaultSpecifier>;
    ExportNamespaceSpecifier?: VisitNode<S, t.ExportNamespaceSpecifier>;
    RestProperty?: VisitNode<S, t.RestProperty>;
    SpreadProperty?: VisitNode<S, t.SpreadProperty>;
    Binary?: VisitNode<S, t.Binary>;
    Scopable?: VisitNode<S, t.Scopable>;
    BlockParent?: VisitNode<S, t.BlockParent>;
    Block?: VisitNode<S, t.Block>;
    Terminatorless?: VisitNode<S, t.Terminatorless>;
    CompletionStatement?: VisitNode<S, t.CompletionStatement>;
    Conditional?: VisitNode<S, t.Conditional>;
    Loop?: VisitNode<S, t.Loop>;
    While?: VisitNode<S, t.While>;
    ExpressionWrapper?: VisitNode<S, t.ExpressionWrapper>;
    For?: VisitNode<S, t.For>;
    ForXStatement?: VisitNode<S, t.ForXStatement>;
    Function?: VisitNode<S, t.Function>;
    FunctionParent?: VisitNode<S, t.FunctionParent>;
    Pureish?: VisitNode<S, t.Pureish>;
    Literal?: VisitNode<S, t.Literal>;
    Immutable?: VisitNode<S, t.Immutable>;
    UserWhitespacable?: VisitNode<S, t.UserWhitespacable>;
    Method?: VisitNode<S, t.Method>;
    ObjectMember?: VisitNode<S, t.ObjectMember>;
    Property?: VisitNode<S, t.Property>;
    UnaryLike?: VisitNode<S, t.UnaryLike>;
    Pattern?: VisitNode<S, t.Pattern>;
    Class?: VisitNode<S, t.Class>;
    ModuleDeclaration?: VisitNode<S, t.ModuleDeclaration>;
    ExportDeclaration?: VisitNode<S, t.ExportDeclaration>;
    ModuleSpecifier?: VisitNode<S, t.ModuleSpecifier>;
    Flow?: VisitNode<S, t.Flow>;
    FlowBaseAnnotation?: VisitNode<S, t.FlowBaseAnnotation>;
    FlowDeclaration?: VisitNode<S, t.FlowDeclaration>;
    JSX?: VisitNode<S, t.JSX>;
    Scope?: VisitNode<S, t.Scopable>;
  }

  export type VisitNode<T, P> = VisitNodeFunction<T, P>|VisitNodeObject<T>;

  export type VisitNodeFunction<T, P> =
      (this: T, path: NodePath<P>, state: any) => void;

  export interface VisitNodeObject<T> {
    enter?(path: NodePath<T>, state: any): void;
    exit?(path: NodePath<T>, state: any): void;
  }
}
