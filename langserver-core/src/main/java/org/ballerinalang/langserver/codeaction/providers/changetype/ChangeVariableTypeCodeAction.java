/*
 * Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.codeaction.providers.changetype;

import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.SingletonTypeSymbol;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.LetVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.codeaction.CodeActionNodeValidator;
import org.ballerinalang.langserver.codeaction.CodeActionUtil;
import org.ballerinalang.langserver.common.ImportsAcceptor;
import org.ballerinalang.langserver.common.constants.CommandConstants;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.FunctionGenerator;
import org.ballerinalang.langserver.common.utils.PositionUtil;
import org.ballerinalang.langserver.commons.CodeActionContext;
import org.ballerinalang.langserver.commons.codeaction.spi.DiagBasedPositionDetails;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Code Action for change variable type.
 *
 * @since 1.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.codeaction.spi.LSCodeActionProvider")
public class ChangeVariableTypeCodeAction extends TypeCastCodeAction {

    public static final String NAME = "Change Variable Type";
    public static final Set<String> DIAGNOSTIC_CODES = Set.of("BCE2066", "BCE2068", "BCE2652", "BCE3931");
    private static final String UNDERSCORE = "_";

    @Override
    public boolean validate(Diagnostic diagnostic, DiagBasedPositionDetails positionDetails,
                            CodeActionContext context) {
        return DIAGNOSTIC_CODES.contains(diagnostic.diagnosticInfo().code()) &&
                CodeActionNodeValidator.validate(context.nodeAtRange());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CodeAction> getCodeActions(Diagnostic diagnostic,
                                           DiagBasedPositionDetails positionDetails,
                                           CodeActionContext context) {

        Optional<TypeSymbol> optionalFoundType;
        if ("BCE2068".equals(diagnostic.diagnosticInfo().code())) {
            optionalFoundType = positionDetails.diagnosticProperty(
                    CodeActionUtil.getDiagPropertyFilterFunction(
                            DiagBasedPositionDetails.DIAG_PROP_INCOMPATIBLE_TYPES_FOUND_SYMBOL_INDEX));
        } else {
            optionalFoundType = positionDetails.diagnosticProperty(
                    DiagBasedPositionDetails.DIAG_PROP_INCOMPATIBLE_TYPES_FOUND_SYMBOL_INDEX);
        }

        if (optionalFoundType.isEmpty() || !isValidType(optionalFoundType.get())) {
            return Collections.emptyList();
        }
        TypeSymbol foundType = optionalFoundType.get();
        if (foundType.typeKind() == TypeDescKind.SINGLETON) {
            foundType = ((SingletonTypeSymbol) foundType).originalType();
        }
        
        // Skip, non-local var declarations
        Optional<NonTerminalNode> variableNode = getVariableOrObjectFieldNode(positionDetails.matchedNode());
        if (variableNode.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<Node> typeNode = getTypeNode(variableNode.get(), context);
        Optional<String> variableName = getVariableName(variableNode.get());
        if (typeNode.isEmpty() || variableName.isEmpty()) {
            return Collections.emptyList();
        }

        // Derive possible types
        Optional<String> typeNodeStr = getTypeNodeStr(typeNode.get());
        List<CodeAction> actions = new ArrayList<>();
        List<TextEdit> importEdits = new ArrayList<>();
        List<String> types;
        if ("BCE3931".equals(diagnostic.diagnosticInfo().code())) {
            Optional<TypeSymbol> typeSymbol = ((TypeDescTypeSymbol) foundType).typeParameter();
            if (typeSymbol.isEmpty()) {
                return Collections.emptyList();
            }
            types = Collections.singletonList((typeSymbol.get().signature()));
        } else {
            types = CodeActionUtil.getPossibleTypes(foundType, importEdits, context);
        }
        for (String type : types) {
            String typeName = FunctionGenerator.processModuleIDsInText(new ImportsAcceptor(context), type, context);
            if (typeNodeStr.isPresent() && typeNodeStr.get().equals(typeName)) {
                // Skip suggesting same type
                continue;
            }
            List<TextEdit> edits = new ArrayList<>();
            edits.add(new TextEdit(PositionUtil.toRange(typeNode.get().lineRange()), typeName));
            String commandTitle;
            if (variableNode.get().kind() == SyntaxKind.CONST_DECLARATION) {
                commandTitle = String.format(CommandConstants.CHANGE_CONST_TYPE_TITLE, variableName.get(), typeName);
            } else {
                commandTitle = String.format(CommandConstants.CHANGE_VAR_TYPE_TITLE, variableName.get(), typeName);
            }
            edits.addAll(importEdits);
            actions.add(CodeActionUtil
                    .createCodeAction(commandTitle, edits, context.fileUri(), CodeActionKind.QuickFix));
        }
        return actions;
    }

    @Override
    public String getName() {
        return NAME;
    }

    private Optional<NonTerminalNode> getVariableOrObjectFieldNode(NonTerminalNode sNode) {
        // Find var node
        if (isVariableNode(sNode) || sNode.kind() == SyntaxKind.OBJECT_FIELD) {
            return Optional.of(sNode);
        } else if (isVariableNode(sNode.parent()) || sNode.parent().kind() == SyntaxKind.OBJECT_FIELD) {
            return Optional.of(sNode.parent());
        } else if (sNode.parent().kind() == SyntaxKind.COLLECT_CLAUSE 
                && sNode.parent().parent().kind() == SyntaxKind.QUERY_EXPRESSION 
                && isVariableNode(sNode.parent().parent().parent())) {
            return Optional.of(sNode.parent().parent().parent());
        }

        return Optional.empty();
    }

    boolean isVariableNode(NonTerminalNode sNode) {
        if (sNode == null || sNode.kind() == SyntaxKind.POSITIONAL_ARG || sNode.kind() == SyntaxKind.NAMED_ARG) {
            return false;
        }

        return sNode.kind() == SyntaxKind.LOCAL_VAR_DECL
                || sNode.kind() == SyntaxKind.MODULE_VAR_DECL
                || sNode.kind() == SyntaxKind.ASSIGNMENT_STATEMENT
                || sNode.kind() == SyntaxKind.CONST_DECLARATION
                || sNode.kind() == SyntaxKind.LET_VAR_DECL
                || sNode.kind() == SyntaxKind.LET_EXPRESSION;
    }

    private Optional<String> getTypeNodeStr(Node node) {
        if (node.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            SimpleNameReferenceNode sRefNode = (SimpleNameReferenceNode) node;
            return Optional.of(sRefNode.name().text());
        } else if (node.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            QualifiedNameReferenceNode qnRefNode = (QualifiedNameReferenceNode) node;
            return Optional.of(qnRefNode.modulePrefix().text() + ":" + qnRefNode.identifier().text());
        } else if (node instanceof BuiltinSimpleNameReferenceNode refNode) {
            // This case occurs with constant declarations with types
            return Optional.of(refNode.name().text());
        }
        return Optional.empty();
    }

    private Optional<Node> getTypeNode(Node matchedNode, CodeActionContext context) {
        switch (matchedNode.kind()) {
            case LOCAL_VAR_DECL:
                return getLocalVarTypeNode((VariableDeclarationNode) matchedNode);
            case MODULE_VAR_DECL:
                return getModuleVarTypeNode((ModuleVariableDeclarationNode) matchedNode);
            case ASSIGNMENT_STATEMENT:
                Optional<VariableSymbol> optVariableSymbol = getVariableSymbol(context, matchedNode);
                if (optVariableSymbol.isEmpty()) {
                    return Optional.empty();
                }
                SyntaxTree syntaxTree = context.currentSyntaxTree().orElseThrow();
                Optional<NonTerminalNode> node = CommonUtil.findNode(optVariableSymbol.get(), syntaxTree);
                if (node.isPresent() && node.get().kind() == SyntaxKind.TYPED_BINDING_PATTERN) {
                    return Optional.of(((TypedBindingPatternNode) node.get()).typeDescriptor());
                } else {
                    return Optional.empty();
                }
            case CONST_DECLARATION:
                ConstantDeclarationNode constDecl = (ConstantDeclarationNode) matchedNode;
                return Optional.ofNullable(constDecl.typeDescriptor().orElse(null));
            case OBJECT_FIELD:
                return getObjectFieldTypeNode((ObjectFieldNode) matchedNode);
            case LET_VAR_DECL:
                return getLetVarDeclTypeNode((LetVariableDeclarationNode) matchedNode);
            case LET_EXPRESSION:
                Node parent = matchedNode.parent();
                return switch (parent.kind()) {
                    case LOCAL_VAR_DECL -> getLocalVarTypeNode((VariableDeclarationNode) parent);
                    case MODULE_VAR_DECL -> getModuleVarTypeNode((ModuleVariableDeclarationNode) parent);
                    case OBJECT_FIELD -> getObjectFieldTypeNode((ObjectFieldNode) parent);
                    case LET_VAR_DECL -> getLetVarDeclTypeNode((LetVariableDeclarationNode) parent);
                    default -> Optional.empty();
                };
            default:
                return Optional.empty();
        }
    }
    
    private Optional<Node> getLocalVarTypeNode(VariableDeclarationNode node) {
        return Optional.of(node.typedBindingPattern().typeDescriptor());
    }

    private Optional<Node> getModuleVarTypeNode(ModuleVariableDeclarationNode node) {
        return Optional.of(node.typedBindingPattern().typeDescriptor());
    }
    
    private Optional<Node> getObjectFieldTypeNode(ObjectFieldNode node) {
        return Optional.of(node.typeName());
    }
    
    private Optional<Node> getLetVarDeclTypeNode(LetVariableDeclarationNode node) {
        return Optional.ofNullable(node.typedBindingPattern().typeDescriptor());
    }

    private Optional<String> getVariableName(Node matchedNode) {
        return switch (matchedNode.kind()) {
            case LOCAL_VAR_DECL -> getVarNameFromBindingPattern(((VariableDeclarationNode) matchedNode)
                    .typedBindingPattern().bindingPattern());
            case MODULE_VAR_DECL -> getVarNameFromBindingPattern(((ModuleVariableDeclarationNode) matchedNode)
                    .typedBindingPattern().bindingPattern());
            case ASSIGNMENT_STATEMENT -> {
                AssignmentStatementNode assignmentStmtNode = (AssignmentStatementNode) matchedNode;
                Node varRef = assignmentStmtNode.varRef();
                if (varRef.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    yield Optional.of(((SimpleNameReferenceNode) varRef).name().text());
                } else if (varRef.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                    yield Optional.of(((QualifiedNameReferenceNode) varRef).identifier().text());
                }
                yield Optional.empty();
            }
            case CONST_DECLARATION -> {
                ConstantDeclarationNode constantDecl = (ConstantDeclarationNode) matchedNode;
                yield Optional.of(constantDecl.variableName().text());
            }
            case OBJECT_FIELD -> getObjectFieldName((ObjectFieldNode) matchedNode);
            case LET_VAR_DECL -> getLetVarName((LetVariableDeclarationNode) matchedNode);
            case LET_EXPRESSION -> {
                Node parent = matchedNode.parent();
                yield switch (parent.kind()) {
                    case LOCAL_VAR_DECL -> getVarNameFromBindingPattern(((VariableDeclarationNode) parent)
                            .typedBindingPattern().bindingPattern());
                    case MODULE_VAR_DECL -> getVarNameFromBindingPattern(((ModuleVariableDeclarationNode) parent)
                            .typedBindingPattern().bindingPattern());
                    case OBJECT_FIELD -> getObjectFieldName((ObjectFieldNode) parent);
                    case LET_VAR_DECL -> getLetVarName((LetVariableDeclarationNode) parent);
                    default -> Optional.empty();
                };
            }
            default -> Optional.empty();
        };
    }

    private Optional<String> getVarNameFromBindingPattern(BindingPatternNode bindingPatternNode) {
        if (bindingPatternNode.kind() == SyntaxKind.WILDCARD_BINDING_PATTERN) {
            return Optional.of(UNDERSCORE);
        }
        if (bindingPatternNode.kind() == SyntaxKind.CAPTURE_BINDING_PATTERN) {
            return Optional.of(((CaptureBindingPatternNode) bindingPatternNode).variableName().text());
        }
        return Optional.empty();
    }

    private Optional<String> getObjectFieldName(ObjectFieldNode node) {
        return Optional.of(node.fieldName().text());
    }
    
    private Optional<String> getLetVarName(LetVariableDeclarationNode node) {
        BindingPatternNode bindingPatternNode = node.typedBindingPattern().bindingPattern();
        return Optional.of(((CaptureBindingPatternNode) bindingPatternNode).variableName().text());
    }

    private boolean isValidType(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.COMPILATION_ERROR || typeSymbol.typeKind() == TypeDescKind.NONE) {
            return false;
        }
        if (typeSymbol.typeKind() == TypeDescKind.MAP) {
            return ((MapTypeSymbol) typeSymbol).typeParam().typeKind() != TypeDescKind.COMPILATION_ERROR;
        }
        if (typeSymbol.typeKind() == TypeDescKind.TABLE) {
            return ((TableTypeSymbol) typeSymbol).rowTypeParameter().typeKind() != TypeDescKind.COMPILATION_ERROR;
        }

        return true;
    }
}
