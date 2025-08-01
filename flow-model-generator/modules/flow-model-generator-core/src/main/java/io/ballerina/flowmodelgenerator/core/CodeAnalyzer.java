/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.ClassFieldSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.ByteArrayLiteralNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CommentNode;
import io.ballerina.compiler.syntax.tree.CommitActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ComputedResourceAccessSegmentNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchGuardNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarator;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OnFailClauseNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.QueryActionNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StartActionNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WaitActionNode;
import io.ballerina.compiler.syntax.tree.WaitFieldNode;
import io.ballerina.compiler.syntax.tree.WaitFieldsListNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.CommentProperty;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.node.AssignBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.CallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FailBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionCall;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.IfBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayloadBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.MethodCall;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.PanicBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RemoteActionCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ResourceActionCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ReturnBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RollbackBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.StartBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.VariableBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.WaitBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayloadBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;

import static io.ballerina.modelgenerator.commons.CommonUtils.isAiModelModule;
import static io.ballerina.modelgenerator.commons.CommonUtils.isAgentClass;

/**
 * Analyzes the source code and generates the flow model.
 *
 * @since 1.0.0
 */
public class CodeAnalyzer extends NodeVisitor {

    // Readonly fields
    private final Project project;
    private final SemanticModel semanticModel;
    private final Map<String, LineRange> dataMappings;
    private final Map<String, LineRange> naturalFunctions;
    private final TextDocument textDocument;
    private final ModuleInfo moduleInfo;
    private final DiagnosticHandler diagnosticHandler;
    private final boolean forceAssign;
    private final String connectionScope;

    // State fields
    private NodeBuilder nodeBuilder;
    private final List<FlowNode> flowNodeList;
    private final Stack<NodeBuilder> flowNodeBuilderStack;
    private TypedBindingPatternNode typedBindingPatternNode;
    private static final String AI_AGENT = "ai";
    private static final String BALLERINAX = "ballerinax";
    public static final String ICON_PATH = CommonUtils.generateIcon("ballerina", "mcp", "0.4.2");
    public static final String MCP_TOOL_KIT = "McpToolKit";
    public static final String MCP_SERVER = "MCP Server";
    public static final String NAME = "name";

    public CodeAnalyzer(Project project, SemanticModel semanticModel, String connectionScope,
                        Map<String, LineRange> dataMappings, Map<String, LineRange> naturalFunctions,
                        TextDocument textDocument, ModuleInfo moduleInfo, boolean forceAssign) {
        this.project = project;
        this.semanticModel = semanticModel;
        this.dataMappings = dataMappings;
        this.naturalFunctions = naturalFunctions;
        this.connectionScope = connectionScope;
        this.textDocument = textDocument;
        this.moduleInfo = moduleInfo;
        this.forceAssign = forceAssign;
        this.flowNodeList = new ArrayList<>();
        this.flowNodeBuilderStack = new Stack<>();
        this.diagnosticHandler = new DiagnosticHandler(semanticModel);
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isEmpty()) {
            return;
        }
        FunctionBodyNode functionBodyNode = functionDefinitionNode.functionBody();

        // Set the function kind to display in the flow model
        FunctionKind kind;
        String functionName = functionDefinitionNode.functionName().text();
        String accessor = null;
        if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            accessor = functionName;
            functionName = getPathString(functionDefinitionNode.relativeResourcePath());
            NonTerminalNode parentNode = getParentNode(functionDefinitionNode);
            if (parentNode instanceof ServiceDeclarationNode serviceDeclarationNode &&
                    isAgent(serviceDeclarationNode)) {
                kind = FunctionKind.AI_CHAT_AGENT;
            } else {
                kind = FunctionKind.RESOURCE;
            }
        } else if (hasQualifier(functionDefinitionNode.qualifierList(), SyntaxKind.REMOTE_KEYWORD)) {
            kind = FunctionKind.REMOTE_FUNCTION;
        } else {
            kind = FunctionKind.FUNCTION;
        }

        startNode(NodeKind.EVENT_START, functionDefinitionNode).codedata()
                .lineRange(functionBodyNode.lineRange())
                .sourceCode(functionDefinitionNode.toSourceCode().strip());

        nodeBuilder.metadata()
                .addData("kind", kind.getValue())
                .addData("label", functionName);
        if (accessor != null) {
            nodeBuilder.metadata().addData("accessor", accessor);
        }

        // Add the function signature to the metadata
        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        List<String> parametersList = functionSignatureNode.parameters().stream()
                .map(parameter -> parameter.toSourceCode().strip())
                .toList();
        if (!parametersList.isEmpty()) {
            nodeBuilder.metadata().addData(FunctionDefinitionBuilder.METADATA_PARAMETERS_KEY, parametersList);
        }
        functionSignatureNode.returnTypeDesc().ifPresent(returnTypeDesc -> nodeBuilder.metadata()
                .addData(FunctionDefinitionBuilder.METADATA_RETURN_KEY, returnTypeDesc.type().toSourceCode().strip()));

        endNode();
        functionBodyNode.accept(this);
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        Optional<ExpressionNode> optExpr = objectFieldNode.expression();
        if (optExpr.isPresent()) {
            optExpr.get().accept(this);
            nodeBuilder.properties()
                    .type(objectFieldNode.typeName(), true)
                    .data(objectFieldNode.fieldName(), false, new HashSet<>());
            endNode(objectFieldNode);
        }
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        functionBodyBlockNode.namedWorkerDeclarator()
                .ifPresent(namedWorkerDeclarator -> namedWorkerDeclarator.accept(this));
        for (Node statementOrComment : functionBodyBlockNode.statementsWithComments()) {
            statementOrComment.accept(this);
        }
    }

    @Override
    public void visit(CommentNode commentNode) {
        Node node = commentNode.getCommentAttachedNode();
        LinePosition startPos = textDocument.linePositionFrom(node.textRangeWithMinutiae().startOffset());
        int offset = 0;
        if (!(node instanceof Token)) {
            offset = node.textRangeWithMinutiae().startOffset();
        }
        LinePosition endPos =
                textDocument.linePositionFrom(commentNode.getLastMinutiae().textRange().endOffset() + offset);
        LineRange commentRange = LineRange.from(node.lineRange().fileName(), startPos, endPos);
        CommentMetadata commentMetadata = new CommentMetadata(String.join(System.lineSeparator(),
                commentNode.getCommentLines()), commentRange);
        genCommentNode(commentMetadata);
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        Optional<ExpressionNode> optExpr = returnStatementNode.expression();
        if (optExpr.isEmpty()) {
            startNode(NodeKind.STOP, returnStatementNode);
        } else {
            ExpressionNode expr = optExpr.get();
            startNode(NodeKind.RETURN, returnStatementNode)
                    .metadata().description(String.format(ReturnBuilder.DESCRIPTION, expr)).stepOut()
                    .properties().expressionOrAction(expr, ReturnBuilder.RETURN_EXPRESSION_DOC, false);
        }
        nodeBuilder.returning();
        endNode(returnStatementNode);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(remoteMethodCallActionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD)) {
            handleExpressionNode(remoteMethodCallActionNode);
            return;
        }
        Optional<ClassSymbol> optClassSymbol = getClassSymbol(remoteMethodCallActionNode.expression());
        if (optClassSymbol.isEmpty()) {
            handleExpressionNode(remoteMethodCallActionNode);
            return;
        }

        String functionName = remoteMethodCallActionNode.methodName().name().text();
        ExpressionNode expressionNode = remoteMethodCallActionNode.expression();
        MethodSymbol functionSymbol = (MethodSymbol) symbol.get();
        ClassSymbol classSymbol = optClassSymbol.get();
        if (isAgentClass(classSymbol)) {
            startNode(NodeKind.AGENT_CALL, expressionNode.parent());
            populateAgentMetaData(expressionNode, remoteMethodCallActionNode, classSymbol);
        } else {
            startNode(NodeKind.REMOTE_ACTION_CALL, expressionNode.parent());
        }
        setFunctionProperties(functionName, expressionNode, remoteMethodCallActionNode, functionSymbol,
                classSymbol.getName().orElseThrow());
    }

    private void populateAgentMetaData(ExpressionNode expressionNode, NonTerminalNode callNode,
                                       ClassSymbol classSymbol) {
        if (isClassField(expressionNode)) {
            ServiceDeclarationNode serviceDeclaration = getParentServiceDeclaration(callNode);
            for (Node member : serviceDeclaration.members()) {
                if (member.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
                    continue;
                }
                FunctionDefinitionNode funcDef = (FunctionDefinitionNode) member;
                if (funcDef.functionName().text().equals("init")) {
                    for (StatementNode statement : ((FunctionBodyBlockNode) funcDef.functionBody()).statements()) {
                        if (statement.kind() != SyntaxKind.ASSIGNMENT_STATEMENT) {
                            continue;
                        }
                        AssignmentStatementNode assignmentStmtNode = (AssignmentStatementNode) statement;
                        if (!assignmentStmtNode.varRef().toSourceCode().trim()
                                .equals(expressionNode.toSourceCode())) {
                            continue;
                        }
                        ImplicitNewExpressionNode newExpressionNode = getNewExpr(assignmentStmtNode.expression());
                        genAgentData(newExpressionNode, classSymbol);
                    }
                }
            }
        } else {
            for (Symbol moduleSymbol : semanticModel.moduleSymbols()) {
                if (moduleSymbol.kind() != SymbolKind.VARIABLE) {
                    continue;
                }
                VariableSymbol variableSymbol = (VariableSymbol) moduleSymbol;
                if (!variableSymbol.getName().orElseThrow().equals(expressionNode.toSourceCode())) {
                    continue;
                }
                Optional<Location> optLocation = variableSymbol.getLocation();
                if (optLocation.isEmpty()) {
                    throw new IllegalStateException("Location not found for the variable symbol: " +
                            variableSymbol);
                }
                NonTerminalNode parent = CommonUtil.findNode(variableSymbol, CommonUtils.getDocument(project,
                        optLocation.get()).syntaxTree()).get().parent().parent();
                if (parent.kind() != SyntaxKind.MODULE_VAR_DECL) {
                    throw new IllegalStateException("Parent is not a module variable declaration: " + parent);
                }
                ModuleVariableDeclarationNode moduleVariableDeclarationNode =
                        (ModuleVariableDeclarationNode) parent;
                Optional<ExpressionNode> optInitializer = moduleVariableDeclarationNode.initializer();
                if (optInitializer.isEmpty()) {
                    throw new IllegalStateException("Initializer not found for the module variable declaration: " +
                            moduleVariableDeclarationNode);
                }
                ImplicitNewExpressionNode newExpressionNode = getNewExpr(optInitializer.get());
                genAgentData(newExpressionNode, classSymbol);
                break;
            }
        }
    }

    private void genAgentData(ImplicitNewExpressionNode newExpressionNode, ClassSymbol classSymbol) {
        Optional<ParenthesizedArgList> argList = newExpressionNode.parenthesizedArgList();
        if (argList.isEmpty()) {
            throw new IllegalStateException("ParenthesizedArgList not found for the new expression: " +
                    newExpressionNode);
        }
        ExpressionNode toolsArg = null;
        ExpressionNode modelArg = null;
        ExpressionNode systemPromptArg = null;
        ExpressionNode memory = null;
        for (FunctionArgumentNode arg : argList.get().arguments()) {
            if (arg.kind() == SyntaxKind.NAMED_ARG) {
                NamedArgumentNode namedArgumentNode = (NamedArgumentNode) arg;
                if (namedArgumentNode.argumentName().name().text().equals("tools")) {
                    toolsArg = namedArgumentNode.expression();
                } else if (namedArgumentNode.argumentName().name().text().equals("model")) {
                    modelArg = namedArgumentNode.expression();
                } else if (namedArgumentNode.argumentName().name().text().equals("systemPrompt")) {
                    systemPromptArg = namedArgumentNode.expression();
                } else if (namedArgumentNode.argumentName().name().text().equals("memory")) {
                    memory = namedArgumentNode.expression();
                }
            }
        }
        if (toolsArg == null) {
            throw new IllegalStateException("Tools argument not found for the new expression: " +
                    newExpressionNode);
        }
        if (modelArg == null) {
            throw new IllegalStateException("Model argument not found for the new expression: " +
                    newExpressionNode);
        }
        if (systemPromptArg == null) {
            throw new IllegalStateException("SystemPrompt argument not found for the new expression: " +
                    newExpressionNode);
        }

        if (toolsArg.kind() == SyntaxKind.LIST_CONSTRUCTOR) {
            List<ToolData> toolsData = new ArrayList<>();
            ListConstructorExpressionNode listCtrExprNode = (ListConstructorExpressionNode) toolsArg;
            for (Node node : listCtrExprNode.expressions()) {
                if (node.kind() != SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    continue;
                }
                SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) node;
                Optional<Symbol> nodeSymbol = semanticModel.symbol(node);
                if (nodeSymbol.isEmpty()) {
                    String toolName = simpleNameReferenceNode.name().text();
                    toolsData.add(new ToolData(toolName, getIcon(toolName), getToolDescription(toolName), null));
                    continue;
                }

                Symbol symbol = nodeSymbol.get();
                String toolName = simpleNameReferenceNode.name().text();
                boolean isMcpToolKit = nodeSymbol
                        .filter(newSymbol -> symbol.kind() == SymbolKind.VARIABLE)
                        .map(newSymbol -> ((VariableSymbol) symbol).typeDescriptor())
                        .filter(typeSymbol -> typeSymbol.getModule().isPresent()
                                && typeSymbol.nameEquals(MCP_TOOL_KIT)
                                && typeSymbol.getModule().get().id().moduleName().equals(AI_AGENT))
                        .isPresent();
                if (isMcpToolKit) {
                    toolsData.add(new ToolData(toolName, ICON_PATH, getToolDescription(""), MCP_SERVER));
                } else {
                    toolName = simpleNameReferenceNode.name().text();
                    toolsData.add(new ToolData(toolName, getIcon(toolName), getToolDescription(toolName), null));
                }
            }
            nodeBuilder.metadata().addData("tools", toolsData);
        }

        if (systemPromptArg.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
            MappingConstructorExpressionNode mappingCtrExprNode =
                    (MappingConstructorExpressionNode) systemPromptArg;
            SeparatedNodeList<MappingFieldNode> fields = mappingCtrExprNode.fields();
            Map<String, String> agentData = new HashMap<>();
            for (MappingFieldNode field : fields) {
                SyntaxKind kind = field.kind();
                if (kind != SyntaxKind.SPECIFIC_FIELD) {
                    continue;
                }
                SpecificFieldNode specificFieldNode = (SpecificFieldNode) field;
                agentData.put(specificFieldNode.fieldName().toString().trim(),
                        specificFieldNode.valueExpr().orElseThrow().toSourceCode());
            }
            nodeBuilder.metadata().addData("agent", agentData);
        }

        if (memory == null) {
            String defaultMemoryManagerName = getDefaultMemoryManagerName(classSymbol);
            nodeBuilder.metadata().addData("memory",
                    new MemoryManagerData(defaultMemoryManagerName, "10"));
        } else if (memory.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
            ExplicitNewExpressionNode newExpr = (ExplicitNewExpressionNode) memory;
            SeparatedNodeList<FunctionArgumentNode> arguments = newExpr.parenthesizedArgList().arguments();
            String size = "";
            if (arguments.size() == 1) {
                size = arguments.get(0).toSourceCode();
            }
            nodeBuilder.metadata().addData("memory",
                    new MemoryManagerData(newExpr.typeDescriptor().toSourceCode(), size));
        }

        ModelData modelUrl = getModelIconUrl(modelArg);
        if (modelUrl != null) {
            nodeBuilder.metadata().addData("model", modelUrl);
        }
    }

    private boolean isClassField(ExpressionNode expr) {
        if (expr.kind() == SyntaxKind.FIELD_ACCESS) {
            return ((FieldAccessExpressionNode) expr).expression().toSourceCode().trim().equals("self");
        }
        return false;
    }

    private ServiceDeclarationNode getParentServiceDeclaration(NonTerminalNode node) {
        NonTerminalNode currentNode = node;
        while (currentNode != null && currentNode.kind() != SyntaxKind.SERVICE_DECLARATION) {
            currentNode = currentNode.parent();
        }
        if (currentNode == null) {
            throw new IllegalStateException("Service declaration not found");
        }
        return (ServiceDeclarationNode) currentNode;
    }

    private NonTerminalNode getParentNode(NonTerminalNode node) {
        NonTerminalNode currentNode = node;
        while (currentNode != null && currentNode.kind() != SyntaxKind.SERVICE_DECLARATION &&
                currentNode.kind() != SyntaxKind.CLASS_DEFINITION) {
            currentNode = currentNode.parent();
        }
        if (currentNode == null) {
            throw new IllegalStateException("Parent node not found");
        }
        return currentNode;
    }

    private void setFunctionProperties(String functionName, ExpressionNode expressionNode,
                                       RemoteMethodCallActionNode remoteMethodCallActionNode,
                                       MethodSymbol functionSymbol, String objName) {
        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .name(functionName)
                .functionSymbol(functionSymbol)
                .semanticModel(semanticModel)
                .userModuleInfo(moduleInfo);
        FunctionData functionData = functionDataBuilder.build();

        nodeBuilder
                .symbolInfo(functionSymbol)
                .metadata()
                .label(functionName)
                .description(functionData.description())
                .stepOut()
                .codedata()
                .nodeInfo(remoteMethodCallActionNode)
                .object(objName)
                .symbol(functionName)
                .stepOut()
                .properties().callConnection(expressionNode, Property.CONNECTION_KEY);
        processFunctionSymbol(remoteMethodCallActionNode, remoteMethodCallActionNode.arguments(), functionSymbol,
                functionData);
    }

    private String getDefaultMemoryManagerName(ClassSymbol classSymbol) {
        Optional<MethodSymbol> initMethodSymbol = classSymbol.initMethod();
        if (initMethodSymbol.isEmpty()) {
            return "";
        }
        Optional<List<ParameterSymbol>> optParams = initMethodSymbol.get().typeDescriptor().params();
        if (optParams.isEmpty()) {
            return "";
        }
        for (ParameterSymbol param : optParams.get()) {
            ParameterKind paramKind = param.paramKind();
            if (paramKind == ParameterKind.INCLUDED_RECORD) {
                TypeSymbol rawType = CommonUtils.getRawType(param.typeDescriptor());
                if (rawType.typeKind() != TypeDescKind.RECORD) {
                    break;
                }
                RecordFieldSymbol recordFieldSymbol =
                        ((RecordTypeSymbol) rawType).fieldDescriptors().get("memory");
                if (recordFieldSymbol == null) {
                    break;
                }
                if (recordFieldSymbol.hasDefaultValue()) {
                    // TODO: This should be derived from the default value of memory parameter
                    return "ai:MessageWindowChatMemory";
                }
            }
        }
        return "";
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(clientResourceAccessActionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            handleExpressionNode(clientResourceAccessActionNode);
            return;
        }
        Optional<ClassSymbol> classSymbol = getClassSymbol(clientResourceAccessActionNode.expression());
        if (classSymbol.isEmpty()) {
            handleExpressionNode(clientResourceAccessActionNode);
            return;
        }

        String functionName = clientResourceAccessActionNode.methodName()
                .map(simpleNameReference -> simpleNameReference.name().text()).orElse("get");
        ExpressionNode expressionNode = clientResourceAccessActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> argumentNodes =
                clientResourceAccessActionNode.arguments().map(ParenthesizedArgList::arguments).orElse(null);

        MethodSymbol functionSymbol = (MethodSymbol) symbol.get();
        startNode(NodeKind.RESOURCE_ACTION_CALL, expressionNode.parent());

        SeparatedNodeList<Node> nodes = clientResourceAccessActionNode.resourceAccessPath();
        ParamUtils.ResourcePathTemplate resourcePathTemplate = ParamUtils.buildResourcePathTemplate(semanticModel,
                functionSymbol, semanticModel.types().ERROR);
        if (CommonUtils.isHttpModule(functionSymbol)) {
            String resourcePath = nodes.stream().map(Node::toSourceCode).collect(Collectors.joining("/"));
            String fullPath = "/" + resourcePath;
            nodeBuilder.properties().resourcePath(fullPath, true);
        } else {
            nodeBuilder.properties().resourcePath(resourcePathTemplate.resourcePathTemplate(), false);
            int idx = 0;
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (nodes.size() <= idx) {
                    break;
                }
                if (node instanceof ComputedResourceAccessSegmentNode computedResourceAccessSegmentNode) {
                    ExpressionNode expr = computedResourceAccessSegmentNode.expression();
                    ParameterData paramResult = resourcePathTemplate.pathParams().get(idx);
                    String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                    nodeBuilder.properties()
                            .custom()
                                .metadata()
                                .label(unescapedParamName)
                                .description(paramResult.description())
                                .stepOut()
                            .codedata()
                                .kind(paramResult.kind().name())
                                .originalName(paramResult.name())
                                .stepOut()
                            .value(expr.toSourceCode())
                            .typeConstraint(paramResult.type())
                            .typeMembers(paramResult.typeMembers())
                            .type(Property.ValueType.EXPRESSION)
                            .editable()
                            .defaultable(paramResult.optional())
                            .stepOut()
                            .addProperty(unescapedParamName);
                    idx++;
                }
            }
        }

        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .name(functionName)
                .functionSymbol(functionSymbol)
                .semanticModel(semanticModel)
                .userModuleInfo(moduleInfo)
                .resourcePath(resourcePathTemplate.resourcePathTemplate())
                .functionResultKind(FunctionData.Kind.RESOURCE);
        FunctionData functionData = functionDataBuilder.build();

        nodeBuilder.symbolInfo(functionSymbol)
                .metadata()
                    .label(functionName)
                    .description(functionData.description())
                    .stepOut()
                .codedata()
                    .nodeInfo(clientResourceAccessActionNode)
                    .object(classSymbol.get().getName().orElse(""))
                    .symbol(functionName)
                    .resourcePath(resourcePathTemplate.resourcePathTemplate())
                    .stepOut()
                .properties()
                .callConnection(expressionNode, Property.CONNECTION_KEY)
                .data(this.typedBindingPatternNode, false, new HashSet<>());
        processFunctionSymbol(clientResourceAccessActionNode, argumentNodes, functionSymbol, functionData);
    }

    private void addRemainingParamsToPropertyMap(Map<String, ParameterData> funcParamMap,
                                                 boolean hasOnlyRestParams) {
        for (Map.Entry<String, ParameterData> entry : funcParamMap.entrySet()) {
            ParameterData paramResult = entry.getValue();
            if (paramResult.kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            String label = paramResult.label();
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = nodeBuilder.properties().custom();
            customPropBuilder
                    .metadata()
                        .label(label == null || label.isEmpty() ? unescapedParamName : label)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .stepOut()
                    .placeholder(paramResult.placeholder())
                    .defaultValue(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .typeMembers(paramResult.typeMembers())
                    .imports(paramResult.importStatements())
                    .editable()
                    .defaultable(paramResult.optional());

            if (paramResult.kind() == ParameterData.Kind.INCLUDED_RECORD_REST) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                unescapedParamName = "additionalValues";
                customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
            } else if (paramResult.kind() == ParameterData.Kind.REST_PARAMETER) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }
    }

    private void calculateFunctionArgs(Map<String, Node> namedArgValueMap,
                                       Queue<Node> positionalArgs,
                                       SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        if (argumentNodes != null) {
            for (FunctionArgumentNode argument : argumentNodes) {
                switch (argument.kind()) {
                    case NAMED_ARG -> {
                        NamedArgumentNode namedArgument = (NamedArgumentNode) argument;
                        namedArgValueMap.put(namedArgument.argumentName().name().text(),
                                namedArgument.expression());
                    }
                    case POSITIONAL_ARG -> positionalArgs.add(((PositionalArgumentNode) argument).expression());
                    default -> {
                        // Ignore the default case
                    }
                }
            }
        }
    }

    private void buildPropsFromFuncCallArgs(SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                            FunctionTypeSymbol functionTypeSymbol,
                                            Map<String, ParameterData> funcParamMap,
                                            Queue<Node> positionalArgs, Map<String, Node> namedArgValueMap) {
        if (argumentNodes == null) { // cl->/path/to/'resource;
            List<ParameterData> functionParameters = funcParamMap.values().stream().toList();
            boolean hasOnlyRestParams = functionParameters.size() == 1;
            for (ParameterData paramResult : functionParameters) {
                ParameterData.Kind paramKind = paramResult.kind();

                if (paramKind.equals(ParameterData.Kind.PATH_PARAM) ||
                        paramKind.equals(ParameterData.Kind.PATH_REST_PARAM)
                        || paramKind.equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                        || paramKind.equals(ParameterData.Kind.INCLUDED_RECORD)) {
                    continue;
                }

                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                String label = paramResult.label();
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = nodeBuilder.properties().custom();
                customPropBuilder
                        .metadata()
                            .label(label == null || label.isEmpty() ? unescapedParamName : label)
                            .description(paramResult.description())
                            .stepOut()
                        .codedata()
                            .kind(paramKind.name())
                            .originalName(paramResult.name())
                            .stepOut()
                        .placeholder(paramResult.placeholder())
                        .defaultValue(paramResult.defaultValue())
                        .typeConstraint(paramResult.type())
                        .typeMembers(paramResult.typeMembers())
                        .imports(paramResult.importStatements())
                        .editable()
                        .defaultable(paramResult.optional());

                if (paramKind == ParameterData.Kind.INCLUDED_RECORD_REST) {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
                } else if (paramKind == ParameterData.Kind.REST_PARAMETER) {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
                } else {
                    if (paramResult.type() instanceof List<?>) {
                        customPropBuilder.type(Property.ValueType.SINGLE_SELECT);
                    } else {
                        customPropBuilder.type(Property.ValueType.EXPRESSION);
                    }
                }
                customPropBuilder
                        .stepOut()
                        .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName));
            }
            return;
        }

        boolean hasOnlyRestParams = funcParamMap.size() == 1;
        if (functionTypeSymbol.restParam().isPresent()) {
            ParameterSymbol restParamSymbol = functionTypeSymbol.restParam().get();
            Optional<List<ParameterSymbol>> paramsOptional = functionTypeSymbol.params();

            if (paramsOptional.isPresent()) {
                List<ParameterSymbol> paramsList = paramsOptional.get();
                int paramCount = paramsList.size(); // param count without rest params
                int argCount = positionalArgs.size();

                List<String> restArgs = new ArrayList<>();
                for (int i = 0; i < paramsList.size(); i++) {
                    ParameterSymbol parameterSymbol = paramsList.get(i);
                    String escapedParamName = parameterSymbol.getName().get();
                    ParameterData paramResult = funcParamMap.get(escapedParamName);
                    if (paramResult == null) {
                        escapedParamName = CommonUtil.escapeReservedKeyword(parameterSymbol.getName().get());
                    }
                    paramResult = funcParamMap.get(escapedParamName);
                    Node paramValue = i < argCount ? positionalArgs.poll()
                            : namedArgValueMap.get(paramResult.name());

                    funcParamMap.remove(parameterSymbol.getName().get());
                    Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                            nodeBuilder.properties().custom();
                    String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                    String value = null;
                    String selectedType = "";
                    if (paramValue != null) {
                        value = paramValue.toSourceCode();
                        Optional<TypeSymbol> paramType = semanticModel.typeOf(paramValue);
                        if (paramType.isPresent()) {
                            if (paramType.get().getModule().isPresent()) {
                                ModuleID id = paramType.get().getModule().get().id();
                                selectedType = CommonUtils.getTypeSignature(paramType.get(), ModuleInfo.from(id));
                            } else {
                                selectedType = CommonUtils.getTypeSignature(paramType.get(), null);
                            }
                        }
                    }
                    String label = paramResult.label();
                    customPropBuilder
                            .metadata()
                                .label(label == null || label.isEmpty() ? unescapedParamName : label)
                                .description(paramResult.description())
                                .stepOut()
                            .type(getPropertyTypeFromParam(parameterSymbol, paramResult))
                            .typeConstraint(paramResult.type())
                            .typeMembers(paramResult.typeMembers(), selectedType)
                            .imports(paramResult.importStatements())
                            .value(value)
                            .placeholder(paramResult.placeholder())
                            .defaultValue(paramResult.defaultValue())
                            .editable()
                            .defaultable(paramResult.optional())
                            .codedata()
                                .kind(paramResult.kind().name())
                                .originalName(paramResult.name())
                                .stepOut()
                            .stepOut()
                            .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName));
                }

                for (int i = paramCount; i < argCount; i++) {
                    restArgs.add(Objects.requireNonNull(positionalArgs.poll()).toSourceCode());
                }
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                String escapedParamName = restParamSymbol.getName().get();
                ParameterData restParamResult = funcParamMap.get(escapedParamName);
                if (restParamResult == null) {
                    restParamResult = funcParamMap.get(CommonUtil.escapeReservedKeyword(
                            restParamSymbol.getName().get()));
                }
                funcParamMap.remove(restParamSymbol.getName().get());
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(restParamResult.name());
                customPropBuilder
                        .metadata()
                            .label(unescapedParamName)
                            .description(restParamResult.description())
                            .stepOut()
                        .type(getPropertyTypeFromParam(restParamSymbol, restParamResult))
                        .typeConstraint(restParamResult.type())
                        .typeMembers(restParamResult.typeMembers())
                        .imports(restParamResult.importStatements())
                        .value(restArgs)
                        .placeholder(restParamResult.placeholder())
                        .defaultValue(restParamResult.defaultValue())
                        .editable()
                        .defaultable(!hasOnlyRestParams)
                        .codedata()
                            .kind(restParamResult.kind().name())
                            .originalName(restParamResult.name())
                            .stepOut()
                        .stepOut()
                        .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName));
            }
            // iterate over functionParamMap
            addRemainingParamsToPropertyMap(funcParamMap, hasOnlyRestParams);
            return;
        }
        Optional<List<ParameterSymbol>> paramsOptional = functionTypeSymbol.params();
        if (paramsOptional.isPresent()) {
            List<ParameterSymbol> paramsList = paramsOptional.get();
            int argCount = positionalArgs.size();

            final List<LinkedHashMap<String, String>> includedRecordRestArgs = new ArrayList<>();
            for (int i = 0; i < paramsList.size(); i++) {
                ParameterSymbol parameterSymbol = paramsList.get(i);
                String escapedParamName = parameterSymbol.getName().get();
                if (!funcParamMap.containsKey(escapedParamName)) {
                    escapedParamName = CommonUtil.escapeReservedKeyword(escapedParamName);
                    if (!funcParamMap.containsKey(escapedParamName)) {
                        continue;
                    }
                }
                ParameterData paramResult = funcParamMap.get(escapedParamName);
                Node paramValue;
                if (i < argCount) {
                    paramValue = positionalArgs.poll();
                } else {
                    paramValue = namedArgValueMap.get(paramResult.name());
                    namedArgValueMap.remove(paramResult.name());
                }
                if (paramResult.kind() == ParameterData.Kind.INCLUDED_RECORD) {
                    if (argumentNodes.size() > i && argumentNodes.get(i).kind() == SyntaxKind.NAMED_ARG) {
                        FunctionArgumentNode argNode = argumentNodes.get(i);
                        funcParamMap.remove(escapedParamName);
                        NamedArgumentNode namedArgumentNode = (NamedArgumentNode) argNode;
                        String argName = namedArgumentNode.argumentName().name().text();
                        if (argName.equals(paramResult.name())) {  // foo("a", b = {})
                            paramResult = funcParamMap.get(escapedParamName);

                            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                                    nodeBuilder.properties().custom();
                            String value = paramValue != null ? paramValue.toSourceCode() : null;
                            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                            Optional<TypeSymbol> paramType = semanticModel.typeOf(paramValue);
                            String selectedType = "";
                            if (paramType.isPresent()) {
                                if (paramType.get().getModule().isPresent()) {
                                    ModuleID id = paramType.get().getModule().get().id();
                                    selectedType = CommonUtils.getTypeSignature(paramType.get(), ModuleInfo.from(id));
                                } else {
                                    selectedType = CommonUtils.getTypeSignature(paramType.get(), null);
                                }
                            }
                            String label = paramResult.label();
                            customPropBuilder
                                    .metadata()
                                        .label(label == null || label.isEmpty() ? unescapedParamName : label)
                                        .description(paramResult.description())
                                        .stepOut()
                                    .type(getPropertyTypeFromParam(parameterSymbol, paramResult))
                                    .typeConstraint(paramResult.type())
                                    .typeMembers(paramResult.typeMembers(), selectedType)
                                    .imports(paramResult.importStatements())
                                    .value(value)
                                    .placeholder(paramResult.placeholder())
                                    .defaultValue(paramResult.defaultValue())
                                    .editable()
                                    .defaultable(paramResult.optional())
                                    .codedata()
                                        .kind(paramResult.kind().name())
                                        .originalName(paramResult.name())
                                        .stepOut()
                                    .stepOut()
                                    .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName), paramValue);
                        } else {
                            if (funcParamMap.containsKey(argName)) { // included record attribute
                                paramResult = funcParamMap.get(argName);
                                funcParamMap.remove(argName);
                                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                                        nodeBuilder.properties().custom();
                                if (paramValue == null) {
                                    paramValue = namedArgValueMap.get(argName);
                                    namedArgValueMap.remove(argName);
                                }
                                String value = null;
                                String selectedType = "";
                                if (paramValue != null) {
                                    value = paramValue.toSourceCode();
                                    Optional<TypeSymbol> paramType = semanticModel.typeOf(paramValue);
                                    if (paramType.isPresent()) {
                                        if (paramType.get().getModule().isPresent()) {
                                            ModuleID id = paramType.get().getModule().get().id();
                                            selectedType = CommonUtils.getTypeSignature(
                                                    paramType.get(), ModuleInfo.from(id));
                                        } else {
                                            selectedType = CommonUtils.getTypeSignature(paramType.get(), null);
                                        }
                                    }
                                }

                                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                                String label = paramResult.label();
                                customPropBuilder
                                        .metadata()
                                            .label(label == null || label.isEmpty() ? unescapedParamName : label)
                                            .description(paramResult.description())
                                            .stepOut()
                                        .type(getPropertyTypeFromParam(parameterSymbol, paramResult))
                                        .typeConstraint(paramResult.type())
                                        .typeMembers(paramResult.typeMembers(), selectedType)
                                        .imports(paramResult.importStatements())
                                        .value(value)
                                        .placeholder(paramResult.placeholder())
                                        .defaultValue(paramResult.defaultValue())
                                        .editable()
                                        .defaultable(paramResult.optional())
                                        .codedata()
                                            .kind(paramResult.kind().name())
                                            .originalName(paramResult.name())
                                            .stepOut()
                                        .stepOut()
                                        .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName), paramValue);

                            }
                        }

                    } else { // positional arg
                        if (paramValue != null) {
                            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                                    nodeBuilder.properties().custom();

                            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                            funcParamMap.remove(escapedParamName);
                            String value = paramValue.toSourceCode();
                            Optional<TypeSymbol> paramType = semanticModel.typeOf(paramValue);
                            String selectedType = "";
                            if (paramType.isPresent()) {
                                if (paramType.get().getModule().isPresent()) {
                                    ModuleID id = paramType.get().getModule().get().id();
                                    selectedType = CommonUtils.getTypeSignature(paramType.get(), ModuleInfo.from(id));
                                } else {
                                    selectedType = CommonUtils.getTypeSignature(paramType.get(), null);
                                }
                            }
                            String label = paramResult.label();
                            customPropBuilder
                                    .metadata()
                                        .label(label == null || label.isEmpty() ? unescapedParamName : label)
                                        .description(paramResult.description())
                                        .stepOut()
                                    .type(getPropertyTypeFromParam(parameterSymbol, paramResult))
                                    .typeConstraint(paramResult.type())
                                    .typeMembers(paramResult.typeMembers(), selectedType)
                                    .imports(paramResult.importStatements())
                                    .value(value)
                                    .placeholder(paramResult.placeholder())
                                    .defaultValue(paramResult.defaultValue())
                                    .editable()
                                    .defaultable(paramResult.optional())
                                    .codedata()
                                        .kind(paramResult.kind().name())
                                        .originalName(paramResult.name())
                                        .stepOut()
                                    .stepOut()
                                    .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName), paramValue);
                            return;
                        }
                    }
                }

                if (paramValue == null && paramResult.kind() == ParameterData.Kind.INCLUDED_RECORD) {
                    funcParamMap.remove(escapedParamName);
                    continue;
                }
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                funcParamMap.remove(escapedParamName);
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                String value = null;
                String selectedType = "";
                if (paramValue != null) {
                    value = paramValue.toSourceCode();
                    Optional<TypeSymbol> paramType = semanticModel.typeOf(paramValue);
                    if (paramType.isPresent()) {
                        if (paramType.get().getModule().isPresent()) {
                            ModuleID id = paramType.get().getModule().get().id();
                            selectedType = CommonUtils.getTypeSignature(paramType.get(), ModuleInfo.from(id));
                        } else {
                            selectedType = CommonUtils.getTypeSignature(paramType.get(), null);
                        }
                    }
                }
                String label = paramResult.label();
                customPropBuilder
                        .metadata()
                            .label(label == null || label.isEmpty() ? unescapedParamName : label)
                            .description(paramResult.description())
                            .stepOut()
                        .type(getPropertyTypeFromParam(parameterSymbol, paramResult))
                        .typeConstraint(paramResult.type())
                        .typeMembers(paramResult.typeMembers(), selectedType)
                        .imports(paramResult.importStatements())
                        .value(value)
                        .placeholder(paramResult.placeholder())
                        .defaultValue(paramResult.defaultValue())
                        .editable()
                        .defaultable(paramResult.optional())
                        .codedata()
                            .kind(paramResult.kind().name())
                            .originalName(paramResult.name())
                            .stepOut()
                        .stepOut()
                        .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName), paramValue);
            }

            for (Map.Entry<String, Node> entry : namedArgValueMap.entrySet()) { // handle remaining named args
                String escapedParamName = CommonUtil.escapeReservedKeyword(entry.getKey());
                if (!funcParamMap.containsKey(escapedParamName)) {
                    LinkedHashMap<String, String> map = new LinkedHashMap<>();
                    map.put(entry.getKey(), entry.getValue().toSourceCode());
                    includedRecordRestArgs.add(map);
                    continue;
                }
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                ParameterData paramResult = funcParamMap.remove(escapedParamName);
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                String value = null;
                String selectedType = "";
                Node paramValue = entry.getValue();
                if (paramValue != null) {
                    value = paramValue.toSourceCode();
                    Optional<TypeSymbol> paramType = semanticModel.typeOf(paramValue);
                    if (paramType.isPresent()) {
                        if (paramType.get().getModule().isPresent()) {
                            ModuleID id = paramType.get().getModule().get().id();
                            selectedType = CommonUtils.getTypeSignature(paramType.get(), ModuleInfo.from(id));
                        } else {
                            selectedType = CommonUtils.getTypeSignature(paramType.get(), null);
                        }
                    }
                }
                String label = paramResult.label();
                customPropBuilder
                        .metadata()
                        .label(label == null || label.isEmpty() ? unescapedParamName : label)
                        .description(paramResult.description())
                        .stepOut()
                        .type(getPropertyTypeFromParam(null, paramResult))
                        .typeConstraint(paramResult.type())
                        .typeMembers(paramResult.typeMembers(), selectedType)
                        .imports(paramResult.importStatements())
                        .value(value)
                        .placeholder(paramResult.placeholder())
                        .defaultValue(paramResult.defaultValue())
                        .editable()
                        .defaultable(paramResult.optional())
                        .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .stepOut()
                        .stepOut()
                        .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName), paramValue);
            }
            ParameterData includedRecordRest = funcParamMap.get("Additional Values");
            if (includedRecordRest != null) {
                funcParamMap.remove("Additional Values");
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(includedRecordRest.name());
                customPropBuilder
                        .metadata()
                            .label(unescapedParamName)
                            .description(includedRecordRest.description())
                            .stepOut()
                        .type(getPropertyTypeFromParam(null, includedRecordRest))
                        .typeConstraint(includedRecordRest.type())
                        .typeMembers(includedRecordRest.typeMembers())
                        .imports(includedRecordRest.importStatements())
                        .value(includedRecordRestArgs)
                        .placeholder(includedRecordRest.placeholder())
                        .defaultValue(includedRecordRest.defaultValue())
                        .editable()
                        .defaultable(includedRecordRest.optional())
                        .codedata()
                            .kind(includedRecordRest.kind().name())
                            .originalName(includedRecordRest.name())
                            .stepOut()
                        .stepOut()
                        .addProperty("additionalValues");
            }
            addRemainingParamsToPropertyMap(funcParamMap, hasOnlyRestParams);
        }
    }

    private void handleCheckFlag(NonTerminalNode node, FunctionTypeSymbol functionTypeSymbol) {
        SyntaxKind parentKind = node.parent().kind();
        if (parentKind == SyntaxKind.CHECK_ACTION || parentKind == SyntaxKind.CHECK_EXPRESSION) {
            nodeBuilder.properties().checkError(true);
        } else {
            functionTypeSymbol.returnTypeDescriptor()
                    .ifPresent(typeSymbol -> {
                        if (CommonUtils.subTypeOf(typeSymbol, semanticModel.types().ERROR)
                                && CommonUtils.withinDoClause(node)) {
                            nodeBuilder.properties().checkError(false);
                        }
                    });
        }
    }

    private Property.ValueType getPropertyTypeFromParam(ParameterSymbol paramSymbol, ParameterData paramData) {
        ParameterData.Kind kind = paramData.kind();
        if (kind == ParameterData.Kind.REST_PARAMETER) {
            return Property.ValueType.EXPRESSION_SET;
        } else if (kind == ParameterData.Kind.INCLUDED_RECORD_REST) {
            return Property.ValueType.MAPPING_EXPRESSION_SET;
        } else if (paramSymbol != null && isSubTypeOfRawTemplate(paramSymbol.typeDescriptor())) {
            return Property.ValueType.RAW_TEMPLATE;
        } else {
            if (paramData.type() instanceof List<?>) {
                return Property.ValueType.SINGLE_SELECT;
            }
            return Property.ValueType.EXPRESSION;
        }
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        startNode(NodeKind.IF, ifElseStatementNode);
        addConditionalBranch(ifElseStatementNode.condition(), ifElseStatementNode.ifBody(), IfBuilder.IF_THEN_LABEL);
        ifElseStatementNode.elseBody().ifPresent(this::analyzeElseBody);
        endNode(ifElseStatementNode);
    }

    private void addConditionalBranch(ExpressionNode condition, BlockStatementNode body, String label) {
        Branch.Builder branchBuilder = startBranch(label, NodeKind.CONDITIONAL, Branch.BranchKind.BLOCK,
                Branch.Repeatable.ONE_OR_MORE).properties().condition(condition).stepOut();
        analyzeBlock(body, branchBuilder);
        endBranch(branchBuilder, body);
    }

    private void analyzeElseBody(Node elseBody) {
        switch (elseBody.kind()) {
            case ELSE_BLOCK -> analyzeElseBody(((ElseBlockNode) elseBody).elseBody());
            case BLOCK_STATEMENT -> {
                Branch.Builder branchBuilder =
                        startBranch(IfBuilder.IF_ELSE_LABEL, NodeKind.ELSE, Branch.BranchKind.BLOCK,
                                Branch.Repeatable.ZERO_OR_ONE);
                analyzeBlock((BlockStatementNode) elseBody, branchBuilder);
                endBranch(branchBuilder, elseBody);
            }
            case IF_ELSE_STATEMENT -> {
                IfElseStatementNode ifElseNode = (IfElseStatementNode) elseBody;
                addConditionalBranch(ifElseNode.condition(), ifElseNode.ifBody(),
                        ifElseNode.condition().toSourceCode().strip());
                ifElseNode.elseBody().ifPresent(this::analyzeElseBody);
            }
            default -> throw new IllegalStateException("Unexpected else body kind: " + elseBody.kind());
        }
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        SeparatedNodeList<FunctionArgumentNode> argumentNodes =
                implicitNewExpressionNode.parenthesizedArgList()
                        .map(ParenthesizedArgList::arguments)
                        .orElse(null);
        checkForNewConnectionOrAgent(implicitNewExpressionNode, argumentNodes);
    }

    @Override
    public void visit(ExplicitNewExpressionNode explicitNewExpressionNode) {
        SeparatedNodeList<FunctionArgumentNode> argumentNodes =
                explicitNewExpressionNode.parenthesizedArgList().arguments();
        checkForNewConnectionOrAgent(explicitNewExpressionNode, argumentNodes);
    }

    private void checkForNewConnectionOrAgent(NewExpressionNode newExpressionNode,
                                              SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        Optional<ClassSymbol> optClassSymbol = getClassSymbol(newExpressionNode);
        if (optClassSymbol.isEmpty()) {
            return;
        }
        ClassSymbol classSymbol = optClassSymbol.get();
        if (isAgentClass(classSymbol)) {
            startNode(NodeKind.AGENT, newExpressionNode);
        } else if (isAIModel(classSymbol)) {
            startNode(NodeKind.CLASS_INIT, newExpressionNode);
        } else if (classSymbol.qualifiers().contains(Qualifier.CLIENT)) {
            startNode(NodeKind.NEW_CONNECTION, newExpressionNode);
        } else if (classSymbol.nameEquals(MCP_TOOL_KIT)) {
            startNode(NodeKind.MCP_TOOLKIT, newExpressionNode);
        } else {
            handleExpressionNode(newExpressionNode);
            return;
        }

        Optional<MethodSymbol> optMethodSymbol = classSymbol.initMethod();
        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .parentSymbol(classSymbol)
                .semanticModel(semanticModel)
                .name(NewConnectionBuilder.INIT_SYMBOL)
                .functionResultKind(FunctionData.Kind.CONNECTOR)
                .userModuleInfo(moduleInfo);

        FunctionData functionData;
        if (optMethodSymbol.isPresent()) {
            MethodSymbol methodSymbol = optMethodSymbol.get();
            functionDataBuilder.functionSymbol(methodSymbol);
            functionData = functionDataBuilder.build();
            processFunctionSymbol(newExpressionNode, argumentNodes, methodSymbol, functionData);
        } else {
            functionData = functionDataBuilder.build();
        }

        String org = functionData.org();
        String packageName = functionData.packageName();
        String name = classSymbol.getName().orElse("");
        nodeBuilder
                .metadata()
                    .label(packageName)
                    .description(functionData.description())
                    .icon(CommonUtils.generateIcon(org, packageName, functionData.version()))
                    .stepOut()
                .codedata()
                    .org(org)
                    .module(packageName)
                    .object(name)
                    .symbol(NewConnectionBuilder.INIT_SYMBOL)
                    .stepOut()
                .properties()
                .scope(connectionScope)
                .checkError(true, NewConnectionBuilder.CHECK_ERROR_DOC, false);
    }

    private Optional<ClassSymbol> getClassSymbol(ExpressionNode newExpressionNode) {
        Optional<TypeSymbol> typeSymbol =
                CommonUtils.getTypeSymbol(semanticModel, newExpressionNode).flatMap(symbol -> {
                    if (symbol.typeKind() == TypeDescKind.UNION) {
                        return ((UnionTypeSymbol) symbol).memberTypeDescriptors().stream()
                                .filter(tSymbol -> !tSymbol.subtypeOf(semanticModel.types().ERROR))
                                .findFirst();
                    }
                    return Optional.of(symbol);
                });
        if (typeSymbol.isEmpty()) {
            return Optional.empty();
        }
        if (typeSymbol.get().typeKind() != TypeDescKind.TYPE_REFERENCE) {
            return Optional.empty();
        }
        Symbol defintionSymbol = ((TypeReferenceTypeSymbol) typeSymbol.get()).definition();
        if (defintionSymbol.kind() != SymbolKind.CLASS) {
            return Optional.empty();
        }
        return Optional.of((ClassSymbol) defintionSymbol);
    }

    @Override
    public void visit(TemplateExpressionNode templateExpressionNode) {
        if (forceAssign) {
            return;
        }
        if (templateExpressionNode.kind() == SyntaxKind.XML_TEMPLATE_EXPRESSION) {
            startNode(NodeKind.XML_PAYLOAD, templateExpressionNode)
                    .metadata()
                    .description(XmlPayloadBuilder.DESCRIPTION)
                    .stepOut()
                    .properties().expression(templateExpressionNode);
        }
    }

    @Override
    public void visit(ByteArrayLiteralNode byteArrayLiteralNode) {
        if (forceAssign) {
            return;
        }
        startNode(NodeKind.BINARY_DATA, byteArrayLiteralNode)
                .metadata()
                .stepOut()
                .properties().expression(byteArrayLiteralNode);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        handleVariableNode(variableDeclarationNode);
    }

    private void handleVariableNode(NonTerminalNode variableDeclarationNode) {
        Optional<ExpressionNode> initializer;
        Optional<Token> finalKeyword;
        switch (variableDeclarationNode.kind()) {
            case LOCAL_VAR_DECL -> {
                VariableDeclarationNode localVariableDeclarationNode =
                        (VariableDeclarationNode) variableDeclarationNode;
                initializer = localVariableDeclarationNode.initializer();
                this.typedBindingPatternNode = localVariableDeclarationNode.typedBindingPattern();
                finalKeyword = localVariableDeclarationNode.finalKeyword();
            }
            case MODULE_VAR_DECL -> {
                ModuleVariableDeclarationNode moduleVariableDeclarationNode =
                        (ModuleVariableDeclarationNode) variableDeclarationNode;
                initializer = moduleVariableDeclarationNode.initializer();
                this.typedBindingPatternNode = moduleVariableDeclarationNode.typedBindingPattern();
                finalKeyword = Optional.empty();
            }
            default -> throw new IllegalStateException("Unexpected variable declaration kind: " +
                    variableDeclarationNode.kind());
        }
        boolean implicit = false;
        if (initializer.isEmpty()) {
            implicit = true;
            startNode(NodeKind.VARIABLE, variableDeclarationNode)
                    .metadata()
                    .description(AssignBuilder.DESCRIPTION)
                    .stepOut()
                    .properties().expressionOrAction(null, VariableBuilder.EXPRESSION_DOC, true);
        } else {
            ExpressionNode initializerNode = initializer.get();
            initializerNode.accept(this);

            // Generate the default expression node if a node is not built
            if (isNodeUnidentified()) {
                implicit = true;
                startNode(NodeKind.VARIABLE, variableDeclarationNode)
                        .metadata()
                        .description(AssignBuilder.DESCRIPTION)
                        .stepOut()
                        .properties().expressionOrAction(initializerNode, VariableBuilder.EXPRESSION_DOC, true);
            }
        }

        // TODO: Find a better way on how we can achieve this
        if (nodeBuilder instanceof DataMapperBuilder) {
            nodeBuilder.properties().data(this.typedBindingPatternNode, new HashSet<>());
        } else if (nodeBuilder instanceof XmlPayloadBuilder) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "xml");
        } else if (nodeBuilder instanceof JsonPayloadBuilder) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "json");
        } else if (nodeBuilder instanceof BinaryBuilder) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "byte[]");
        } else if (nodeBuilder instanceof NewConnectionBuilder) {
            nodeBuilder.properties()
                    .dataVariable(this.typedBindingPatternNode, NewConnectionBuilder.CONNECTION_NAME_LABEL,
                            NewConnectionBuilder.CONNECTION_TYPE_LABEL, false, new HashSet<>(), true);
        } else if (nodeBuilder instanceof RemoteActionCallBuilder || nodeBuilder instanceof ResourceActionCallBuilder) {
            nodeBuilder.properties()
                    .dataVariable(this.typedBindingPatternNode, Property.RESULT_NAME, Property.RESULT_TYPE_LABEL,
                            Property.RESULT_DOC, false, new HashSet<>(), true);
        } else if (nodeBuilder instanceof FunctionCall || nodeBuilder instanceof MethodCall) {
            nodeBuilder.properties()
                    .dataVariable(this.typedBindingPatternNode, Property.RESULT_NAME, Property.RESULT_TYPE_LABEL,
                            Property.RESULT_DOC, false, new HashSet<>(), false);
        } else {
            nodeBuilder.properties().dataVariable(this.typedBindingPatternNode, implicit, new HashSet<>());
        }
        finalKeyword.ifPresent(token -> nodeBuilder.flag(FlowNode.NODE_FLAG_FINAL));
        endNode(variableDeclarationNode);
        this.typedBindingPatternNode = null;
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        handleVariableNode(moduleVariableDeclarationNode);
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        ExpressionNode expression = assignmentStatementNode.expression();
        expression.accept(this);

        if (isNodeUnidentified()) {
            startNode(NodeKind.ASSIGN, assignmentStatementNode)
                    .metadata()
                    .description(AssignBuilder.DESCRIPTION)
                    .stepOut()
                    .properties()
                    .expressionOrAction(expression, AssignBuilder.EXPRESSION_DOC, false);

            nodeBuilder.properties().custom()
                    .metadata()
                        .label(AssignBuilder.VARIABLE_LABEL)
                        .description(AssignBuilder.VARIABLE_DOC)
                        .stepOut()
                    .type(Property.ValueType.LV_EXPRESSION)
                    .value(CommonUtils.getVariableName(assignmentStatementNode.varRef()))
                    .editable()
                    .stepOut()
                    .addProperty(Property.VARIABLE_KEY);
        }
        endNode(assignmentStatementNode);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        handleDefaultStatementNode(compoundAssignmentStatementNode);
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        handleDefaultStatementNode(blockStatementNode);
    }

    @Override
    public void visit(QueryActionNode queryActionNode) {
        handleDefaultStatementNode(queryActionNode);
    }

    @Override
    public void visit(BreakStatementNode breakStatementNode) {
        startNode(NodeKind.BREAK, breakStatementNode);
        endNode(breakStatementNode);
    }

    @Override
    public void visit(FailStatementNode failStatementNode) {
        startNode(NodeKind.FAIL, failStatementNode)
                .properties().expression(failStatementNode.expression(), FailBuilder.FAIL_EXPRESSION_DOC, false,
                        TypesGenerator.TYPE_ERROR);
        endNode(failStatementNode);
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        expressionStatementNode.expression().accept(this);
        if (isNodeUnidentified()) {
            handleExpressionNode(expressionStatementNode);
        }
        endNode(expressionStatementNode);
    }

    @Override
    public void visit(ContinueStatementNode continueStatementNode) {
        startNode(NodeKind.CONTINUE, continueStatementNode);
        endNode(continueStatementNode);
    }

    @Override
    public void visit(MethodCallExpressionNode methodCallExpressionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(methodCallExpressionNode);
        if (symbol.isEmpty() || !(symbol.get() instanceof FunctionSymbol functionSymbol)) {
            handleExpressionNode(methodCallExpressionNode);
            return;
        }

        // Consider lang lib methods as a variable node with an expression
        if (CommonUtils.isValueLangLibFunction(functionSymbol)) {
            return;
        }

        Optional<ClassSymbol> optClassSymbol = getClassSymbol(methodCallExpressionNode.expression());
        if (optClassSymbol.isEmpty()) {
            handleExpressionNode(methodCallExpressionNode);
            return;
        }

        ExpressionNode expressionNode = methodCallExpressionNode.expression();
        NameReferenceNode nameReferenceNode = methodCallExpressionNode.methodName();
        String functionName = getIdentifierName(nameReferenceNode);
        ClassSymbol classSymbol = optClassSymbol.get();
        if (isAgentClass(classSymbol)) {
            startNode(NodeKind.AGENT_CALL, expressionNode.parent());
            populateAgentMetaData(expressionNode, methodCallExpressionNode, classSymbol);
        } else {
            startNode(NodeKind.METHOD_CALL, methodCallExpressionNode.parent());
        }

        if (CommonUtils.isDefaultPackage(functionSymbol, moduleInfo)) {
            functionSymbol.getLocation()
                    .flatMap(location -> CommonUtil.findNode(functionSymbol,
                            CommonUtils.getDocument(project, location).syntaxTree()))
                    .ifPresent(node -> nodeBuilder.properties().view(node.lineRange()));
        }

        FunctionDataBuilder functionDataBuilder =
                new FunctionDataBuilder()
                        .name(functionName)
                        .functionSymbol(functionSymbol)
                        .semanticModel(semanticModel)
                        .userModuleInfo(moduleInfo);
        FunctionData functionData = functionDataBuilder.build();

        nodeBuilder
                .symbolInfo(functionSymbol)
                    .metadata()
                    .label(functionName)
                    .description(functionData.description())
                    .stepOut()
                .codedata()
                    .symbol(functionName)
                    .object(classSymbol.getName().orElse(""));
        if (classSymbol.qualifiers().contains(Qualifier.CLIENT)) {
            nodeBuilder.properties().callConnection(expressionNode, Property.CONNECTION_KEY);
        } else {
            nodeBuilder.properties().callExpression(expressionNode, Property.CONNECTION_KEY);
        }
        processFunctionSymbol(methodCallExpressionNode, methodCallExpressionNode.arguments(), functionSymbol,
                functionData);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode);
        if (symbol.isEmpty() || symbol.get().kind() != SymbolKind.FUNCTION) {
            handleExpressionNode(functionCallExpressionNode);
            return;
        }

        FunctionSymbol functionSymbol = (FunctionSymbol) symbol.get();

        NameReferenceNode nameReferenceNode = functionCallExpressionNode.functionName();
        String functionName = getIdentifierName(nameReferenceNode);

        if (dataMappings.containsKey(functionName)) {
            startNode(NodeKind.DATA_MAPPER_CALL, functionCallExpressionNode.parent());
        } else if (isAgentClass(symbol.get())) {
            startNode(NodeKind.AGENT_CALL, functionCallExpressionNode.parent());
        } else if (naturalFunctions.containsKey(functionName)) {
            startNode(NodeKind.NP_FUNCTION_CALL, functionCallExpressionNode.parent());
        } else {
            startNode(NodeKind.FUNCTION_CALL, functionCallExpressionNode.parent());
        }

        if (CommonUtils.isDefaultPackage(functionSymbol, moduleInfo)) {
            functionSymbol.getLocation()
                    .flatMap(location -> CommonUtil.findNode(functionSymbol,
                            CommonUtils.getDocument(project, location).syntaxTree()))
                    .ifPresent(node -> nodeBuilder.properties().view(node.lineRange()));
        }

        FunctionDataBuilder functionDataBuilder =
                new FunctionDataBuilder()
                        .name(functionName)
                        .functionSymbol(functionSymbol)
                        .semanticModel(semanticModel)
                        .userModuleInfo(moduleInfo);
        FunctionData functionData = functionDataBuilder.build();

        processFunctionSymbol(functionCallExpressionNode, functionCallExpressionNode.arguments(), functionSymbol,
                functionData);

        nodeBuilder
                .symbolInfo(functionSymbol)
                .metadata()
                .label(functionName)
                .description(functionData.description())
                .stepOut()
                .codedata().symbol(functionName);
    }

    private void processFunctionSymbol(NonTerminalNode callNode, SeparatedNodeList<FunctionArgumentNode> arguments,
                                       FunctionSymbol functionSymbol, FunctionData functionData) {
        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();
        calculateFunctionArgs(namedArgValueMap, positionalArgs, arguments);

        Map<String, ParameterData> funcParamMap = new LinkedHashMap<>();
        Map<String, ParameterData> typeInferParamMap = new LinkedHashMap<>();
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();

        functionData.parameters().forEach((key, paramResult) -> {
            if (paramResult.kind() == ParameterData.Kind.PATH_PARAM) {
                // Skip if `path` param
                return;
            }

            if (paramResult.kind() == ParameterData.Kind.PARAM_FOR_TYPE_INFER) {
                typeInferParamMap.put(key, paramResult);
                return;
            }

            funcParamMap.put(key, paramResult);
        });

        buildPropsFromFuncCallArgs(arguments, functionTypeSymbol, funcParamMap, positionalArgs, namedArgValueMap);
        handleCheckFlag(callNode, functionTypeSymbol);

        // Process PARAM_FOR_TYPE_INFER parameters at the end
        typeInferParamMap.forEach((key, paramResult) -> {
            String returnType = functionData.returnType();

            // Derive the value of the inferred type name
            String inferredTypeName;
            // Check if the value exists in the named arg map
            Node node = namedArgValueMap.get(key);
            if (node != null) {
                inferredTypeName = node.toSourceCode();
            } else if (typedBindingPatternNode == null) {
                // Get the default value if the variable is absent
                inferredTypeName = paramResult.placeholder();
            } else {
                // Derive the inferred type from the variable type
                Optional<Symbol> symbol = semanticModel.symbol(typedBindingPatternNode);
                if (symbol.isEmpty() || symbol.get().kind() != SymbolKind.VARIABLE) {
                    return;
                }
                String variableType =
                        CommonUtils.getTypeSignature(((VariableSymbol) symbol.get()).typeDescriptor(), moduleInfo);

                inferredTypeName = deriveInferredType(variableType, returnType, key);
            }

            // Generate the property of the inferred type param
            nodeBuilder.codedata().inferredReturnType(functionData.returnError() ? returnType : null);
            CallBuilder.buildInferredTypeProperty(nodeBuilder, paramResult, inferredTypeName);
        });
    }

    private static String deriveInferredType(String variableType, String returnType, String key) {
        int keyIndex = returnType.indexOf(key);
        if (keyIndex == -1) {
            // If key is not found, fallback to returning variableType.
            return variableType;
        }
        String prefix = returnType.substring(0, keyIndex);
        String suffix = returnType.substring(keyIndex + key.length());

        // Check if variableType has the same structure as returnType.
        if (variableType.startsWith(prefix) && variableType.endsWith(suffix)) {
            return variableType.substring(prefix.length(), variableType.length() - suffix.length());
        }
        // If the structure doesn't match, return variableType as fallback.
        return variableType;
    }

    private boolean isAIModel(ClassSymbol classSymbol) {
        Optional<ModuleSymbol> optModule = classSymbol.getModule();
        if (optModule.isEmpty()) {
            return false;
        }
        ModuleID id = optModule.get().id();
        if (!isAiModelModule(id.orgName(), id.packageName())) {
            return false;
        }

        for (TypeSymbol typeSymbol : classSymbol.typeInclusions()) {
            Optional<String> optName = typeSymbol.getName();
            if (optName.isPresent()) {
                String name = optName.get();
                if (name.equals("ModelProvider") || name.equals("MemoryManager")) {
                    return true;
                }
            }
        }
        return false;
    }

    private ModelData getModelIconUrl(ExpressionNode expressionNode) {
        if (expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            Optional<Symbol> optSymbol = semanticModel.symbol(expressionNode);
            if (optSymbol.isEmpty()) {
                return null;
            }
            Symbol symbol = optSymbol.get();
            Optional<String> symbolName;
            if (symbol.kind() == SymbolKind.VARIABLE) {
                symbolName = ((VariableSymbol) symbol).typeDescriptor().getName();
            } else if (symbol.kind() == SymbolKind.CLASS_FIELD) {
                symbolName = ((ClassFieldSymbol) symbol).typeDescriptor().getName();
            } else {
                return null;
            }

            Optional<ModuleSymbol> optModule = symbol.getModule();
            if (optModule.isEmpty()) {
                return null;
            }
            ModuleID id = optModule.get().id();
            return new ModelData(optSymbol.get().getName().orElseThrow(),
                    CommonUtils.generateIcon(id.moduleName(), id.packageName(), id.version()), symbolName.orElse(""));
        } else if (expressionNode.kind() == SyntaxKind.FIELD_ACCESS) {
            FieldAccessExpressionNode fieldAccessExpressionNode = (FieldAccessExpressionNode) expressionNode;
            return getModelIconUrl(fieldAccessExpressionNode.fieldName());
        }
        return null;
    }

    private static String getIdentifierName(NameReferenceNode nameReferenceNode) {
        return switch (nameReferenceNode.kind()) {
            case QUALIFIED_NAME_REFERENCE -> ((QualifiedNameReferenceNode) nameReferenceNode).identifier().text();
            case SIMPLE_NAME_REFERENCE -> ((SimpleNameReferenceNode) nameReferenceNode).name().text();
            default -> "";
        };
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        startNode(NodeKind.WHILE, whileStatementNode)
                .properties().condition(whileStatementNode.condition());

        BlockStatementNode whileBody = whileStatementNode.whileBody();
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.CONDITIONAL, Branch.BranchKind.BLOCK,
                        Branch.Repeatable.ONE);
        analyzeBlock(whileBody, branchBuilder);
        endBranch(branchBuilder, whileBody);
        whileStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(whileStatementNode);
    }

    private void processOnFailClause(OnFailClauseNode onFailClauseNode) {
        Branch.Builder branchBuilder =
                startBranch(Branch.ON_FAILURE_LABEL, NodeKind.ON_FAILURE, Branch.BranchKind.BLOCK,
                        Branch.Repeatable.ZERO_OR_ONE);
        if (onFailClauseNode.typedBindingPattern().isPresent()) {
            branchBuilder.properties().ignore(false).onErrorVariable(onFailClauseNode.typedBindingPattern().get());
        }
        BlockStatementNode onFailClauseBlock = onFailClauseNode.blockStatement();
        analyzeBlock(onFailClauseBlock, branchBuilder);
        endBranch(branchBuilder, onFailClauseBlock);
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        startNode(NodeKind.PANIC, panicStatementNode)
                .properties().expression(panicStatementNode.expression(), PanicBuilder.PANIC_EXPRESSION_DOC, false,
                        TypesGenerator.TYPE_ERROR);
        endNode(panicStatementNode);
    }

    @Override
    public void visit(LocalTypeDefinitionStatementNode localTypeDefinitionStatementNode) {
        handleDefaultStatementNode(localTypeDefinitionStatementNode
        );
    }

    @Override
    public void visit(StartActionNode startActionNode) {
        startNode(NodeKind.START, startActionNode).properties()
                .expressionOrAction(startActionNode.expression(), StartBuilder.START_EXPRESSION_DOC, false);
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        startNode(NodeKind.LOCK, lockStatementNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode lockBody = lockStatementNode.blockStatement();
        analyzeBlock(lockBody, branchBuilder);
        endBranch(branchBuilder, lockBody);
        lockStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(lockStatementNode);
    }

    @Override
    public void visit(ForkStatementNode forkStatementNode) {
        startNode(NodeKind.FORK, forkStatementNode);
        forkStatementNode.namedWorkerDeclarations().forEach(this::visit);
        endNode(forkStatementNode);
    }

    @Override
    public void visit(NamedWorkerDeclarator namedWorkerDeclarator) {
        // Analyze the worker init statements
        namedWorkerDeclarator.workerInitStatements().forEach(statement -> statement.accept(this));

        // Generate a parallel flow node for the named workers
        startNode(NodeKind.PARALLEL_FLOW);
        NodeList<NamedWorkerDeclarationNode> workers = namedWorkerDeclarator.namedWorkerDeclarations();
        LineRange startLineRange = workers.get(0).lineRange();
        LinePosition endLine = workers.get(workers.size() - 1).lineRange().endLine();
        workers.forEach(this::visit);
        nodeBuilder.codedata()
                .lineRange(LineRange.from(startLineRange.fileName(), startLineRange.startLine(), endLine));
        endNode();
    }

    @Override
    public void visit(NamedWorkerDeclarationNode namedWorkerDeclarationNode) {
        Branch.Builder workerBranchBuilder =
                startBranch(namedWorkerDeclarationNode.workerName().text(), NodeKind.WORKER, Branch.BranchKind.WORKER,
                        Branch.Repeatable.ONE_OR_MORE);

        // Set the properties of the worker branch
        Optional<Node> returnTypeDesc = namedWorkerDeclarationNode.returnTypeDesc();
        String type;
        if (returnTypeDesc.isPresent() && returnTypeDesc.get().kind() == SyntaxKind.RETURN_TYPE_DESCRIPTOR) {
            ReturnTypeDescriptorNode returnTypeDescriptorNode = (ReturnTypeDescriptorNode) returnTypeDesc.get();
            type = returnTypeDescriptorNode.type().toSourceCode().strip();
        } else {
            type = "";
        }
        workerBranchBuilder.properties()
                .data(namedWorkerDeclarationNode.workerName(), Property.WORKER_NAME, Property.WORKER_DOC,
                        "worker", false)
                .returnType(type);

        // Analyze the body of the worker
        analyzeBlock(namedWorkerDeclarationNode.workerBody(), workerBranchBuilder);
        endBranch(workerBranchBuilder, namedWorkerDeclarationNode);
    }

    @Override
    public void visit(WaitActionNode waitActionNode) {
        startNode(NodeKind.WAIT, waitActionNode);

        // Capture the future nodes associated with the wait node
        boolean waitAll = false;
        Node waitFutureExpr = waitActionNode.waitFutureExpr();
        List<Node> nodes = new ArrayList<>();
        switch (waitFutureExpr.kind()) {
            case BINARY_EXPRESSION -> {
                BinaryExpressionNode binaryExpressionNode = (BinaryExpressionNode) waitFutureExpr;
                Stack<Node> futuresStack = new Stack<>();

                // Capture the right most future
                futuresStack.push(binaryExpressionNode.rhsExpr());

                // Build the stack for the left futures, starting from the right
                Node lhsNode = binaryExpressionNode.lhsExpr();
                while (lhsNode.kind() == SyntaxKind.BINARY_EXPRESSION) {
                    BinaryExpressionNode nestedBinary = (BinaryExpressionNode) lhsNode;
                    futuresStack.push(nestedBinary.rhsExpr());
                    lhsNode = nestedBinary.lhsExpr();
                }
                futuresStack.push(lhsNode);

                // Add the futures to the node list in reverse order from the stack
                while (!futuresStack.isEmpty()) {
                    nodes.add(futuresStack.pop());
                }
            }
            case WAIT_FIELDS_LIST -> {
                WaitFieldsListNode waitFieldsListNode = (WaitFieldsListNode) waitFutureExpr;
                for (Node field : waitFieldsListNode.waitFields()) {
                    nodes.add(field);
                }
                waitAll = true;
            }
            default -> nodes.add(waitFutureExpr);
        }

        // Generate the properties for the futures
        nodeBuilder.properties().waitAll(waitAll).nestedProperty();
        Node waitField;
        ExpressionNode expressionNode;
        int i = 1;
        for (Node n : nodes) {
            if (n.kind() == SyntaxKind.WAIT_FIELD) {
                waitField = ((WaitFieldNode) n).fieldName();
                expressionNode = ((WaitFieldNode) n).waitFutureExpr();
            } else {
                waitField = null;
                expressionNode = (ExpressionNode) n;
            }
            nodeBuilder.properties()
                    .nestedProperty()
                    .waitField(waitField)
                    .expression(expressionNode)
                    .endNestedProperty(Property.ValueType.FIXED_PROPERTY, WaitBuilder.FUTURE_KEY + i++,
                            WaitBuilder.FUTURE_LABEL, WaitBuilder.FUTURE_DOC);
        }

        nodeBuilder.properties().endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, WaitBuilder.FUTURES_KEY,
                WaitBuilder.FUTURES_LABEL, WaitBuilder.FUTURES_DOC);
    }

    @Override
    public void visit(TransactionStatementNode transactionStatementNode) {
        startNode(NodeKind.TRANSACTION, transactionStatementNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode blockStatementNode = transactionStatementNode.blockStatement();
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        transactionStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(transactionStatementNode);
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        startNode(NodeKind.FOREACH, forEachStatementNode)
                .properties()
                .dataVariable(forEachStatementNode.typedBindingPattern(), new HashSet<>())
                .collection(forEachStatementNode.actionOrExpressionNode());
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode blockStatementNode = forEachStatementNode.blockStatement();
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        forEachStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(forEachStatementNode);
    }

    @Override
    public void visit(RollbackStatementNode rollbackStatementNode) {
        startNode(NodeKind.ROLLBACK, rollbackStatementNode);
        Optional<ExpressionNode> optExpr = rollbackStatementNode.expression();
        if (optExpr.isPresent()) {
            ExpressionNode expr = optExpr.get();
            expr.accept(this);
            nodeBuilder.properties().expression(expr, RollbackBuilder.ROLLBACK_EXPRESSION_DOC);
        }
        endNode(rollbackStatementNode);
    }

    @Override
    public void visit(RetryStatementNode retryStatementNode) {
        int retryCount = retryStatementNode.arguments().isEmpty() ? 3 :
                Integer.parseInt(retryStatementNode.arguments()
                        .map(arg -> arg.arguments().get(0)).get().toString());

        StatementNode statementNode = retryStatementNode.retryBody();
        if (statementNode.kind() == SyntaxKind.BLOCK_STATEMENT) {
            startNode(NodeKind.RETRY, retryStatementNode)
                    .properties().retryCount(retryCount);

            Branch.Builder branchBuilder =
                    startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
            analyzeBlock((BlockStatementNode) statementNode, branchBuilder);
            endBranch(branchBuilder, statementNode);
            retryStatementNode.onFailClause().ifPresent(this::processOnFailClause);
            endNode(retryStatementNode);
        } else { // retry transaction node
            TransactionStatementNode transactionStatementNode = (TransactionStatementNode) statementNode;
            BlockStatementNode blockStatementNode = transactionStatementNode.blockStatement();
            startNode(NodeKind.TRANSACTION, retryStatementNode)
                    .properties().retryCount(retryCount);
            Branch.Builder branchBuilder =
                    startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
            analyzeBlock(blockStatementNode, branchBuilder);
            endBranch(branchBuilder, blockStatementNode);
            transactionStatementNode.onFailClause().ifPresent(this::processOnFailClause);
            endNode(retryStatementNode);
        }
    }

    @Override
    public void visit(CommitActionNode commitActionNode) {
        startNode(NodeKind.COMMIT, commitActionNode);
        endNode();
    }

    @Override
    public void visit(MatchStatementNode matchStatementNode) {
        startNode(NodeKind.MATCH, matchStatementNode)
                .properties().condition(matchStatementNode.condition());

        NodeList<MatchClauseNode> matchClauseNodes = matchStatementNode.matchClauses();
        for (MatchClauseNode matchClauseNode : matchClauseNodes) {
            Optional<MatchGuardNode> matchGuardNode = matchClauseNode.matchGuard();
            CommentProperty commentProperty = new CommentProperty();
            String pattern = matchClauseNode.matchPatterns().stream()
                    .map(p -> removeLeadingAndTrailingMinutiae(p, commentProperty))
                    .collect(Collectors.joining("|"));
            String label = pattern;
            if (matchGuardNode.isPresent()) {
                label += " " + matchGuardNode.get().toSourceCode().strip();
            }

            Branch.Builder branchBuilder = startBranch(label, NodeKind.CONDITIONAL, Branch.BranchKind.BLOCK,
                    Branch.Repeatable.ONE_OR_MORE)
                    .properties().patterns(matchClauseNode, pattern, commentProperty).stepOut();

            matchGuardNode.ifPresent(guard -> branchBuilder.properties()
                    .expression(guard.expression(), Property.GUARD_KEY, Property.GUARD_DOC));
            analyzeBlock(matchClauseNode.blockStatement(), branchBuilder);
            endBranch(branchBuilder, matchClauseNode.blockStatement());
        }

        matchStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(matchStatementNode);
    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        Optional<OnFailClauseNode> optOnFailClauseNode = doStatementNode.onFailClause();
        BlockStatementNode blockStatementNode = doStatementNode.blockStatement();
        if (optOnFailClauseNode.isEmpty()) {
            handleDefaultStatementNode(doStatementNode);
            return;
        }

        startNode(NodeKind.ERROR_HANDLER, doStatementNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        processOnFailClause(optOnFailClauseNode.get());
        endNode(doStatementNode);
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        checkExpressionNode.expression().accept(this);
        if (isNodeUnidentified()) {
            return;
        }

        String checkText = checkExpressionNode.checkKeyword().text();
        switch (checkText) {
            case Constants.CHECK -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKED);
            case Constants.CHECKPANIC -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKPANIC);
            default -> throw new IllegalStateException("Unexpected value: " + checkText);

        }
        nodeBuilder.codedata().nodeInfo(checkExpressionNode);
    }

    @Override
    public void visit(MappingConstructorExpressionNode mappingCtrExprNode) {
        handleConstructorExpressionNode(mappingCtrExprNode);
    }

    @Override
    public void visit(ListConstructorExpressionNode listCtrExprNode) {
        handleConstructorExpressionNode(listCtrExprNode);
    }

    private void handleConstructorExpressionNode(ExpressionNode constructorExprNode) {
        NonTerminalNode parent = constructorExprNode.parent();
        SyntaxKind kind = parent.kind();

        if (kind != SyntaxKind.ASSIGNMENT_STATEMENT && kind != SyntaxKind.MODULE_VAR_DECL &&
                kind != SyntaxKind.LOCAL_VAR_DECL) {
            return;
        }

        Optional<Symbol> parentSymbol = semanticModel.symbol(parent);
        if (parentSymbol.isPresent() && CommonUtils.getRawType(
                ((VariableSymbol) parentSymbol.get()).typeDescriptor()).typeKind() == TypeDescKind.JSON &&
                !forceAssign) {
            startNode(NodeKind.JSON_PAYLOAD, constructorExprNode)
                    .metadata()
                    .description(JsonPayloadBuilder.DESCRIPTION)
                    .stepOut()
                    .properties().expression(constructorExprNode);
        }
    }
    // Utility methods

    /**
     * It's the responsibility of the parent node to add the children nodes when building the diagram. Hence, the method
     * only adds the node to the diagram if there is no active parent node which is building its branches.
     */
    private void endNode(Node node) {
        nodeBuilder.codedata().nodeInfo(node);
        endNode();
    }

    private void endNode() {
        if (this.flowNodeBuilderStack.isEmpty()) {
            this.flowNodeList.add(buildNode());
        }
    }

    private NodeBuilder startNode(NodeKind kind) {
        this.nodeBuilder = NodeBuilder.getNodeFromKind(kind)
                .semanticModel(semanticModel)
                .defaultModuleName(moduleInfo);
        return this.nodeBuilder;
    }

    private NodeBuilder startNode(NodeKind kind, Node node) {
        this.nodeBuilder = NodeBuilder.getNodeFromKind(kind)
                .semanticModel(semanticModel)
                .diagnosticHandler(diagnosticHandler)
                .defaultModuleName(moduleInfo);
        diagnosticHandler.handle(nodeBuilder,
                node instanceof ExpressionNode ? node.parent().lineRange() : node.lineRange(), false);
        return this.nodeBuilder;
    }

    /**
     * Builds the flow node and resets the node builder.
     *
     * @return the built flow node
     */
    private FlowNode buildNode() {
        FlowNode node = nodeBuilder.build();
        this.nodeBuilder = null;
        return node;
    }

    /**
     * Starts a new branch and sets the node builder to the starting node of the branch.
     */
    private Branch.Builder startBranch(String label, NodeKind node, Branch.BranchKind kind,
                                       Branch.Repeatable repeatable) {
        this.flowNodeBuilderStack.push(nodeBuilder);
        this.nodeBuilder = null;
        return new Branch.Builder()
                .semanticModel(semanticModel)
                .defaultModuleName(moduleInfo)
                .diagnosticHandler(diagnosticHandler)
                .codedata().node(node).stepOut()
                .label(label)
                .kind(kind)
                .repeatable(repeatable);
    }

    /**
     * Ends the current branch and sets the node builder to the parent node.
     */
    private void endBranch(Branch.Builder branchBuilder, Node node) {
        branchBuilder.codedata().nodeInfo(node);
        nodeBuilder = this.flowNodeBuilderStack.pop();
        nodeBuilder.branch(branchBuilder.build());
    }

    private boolean isNodeUnidentified() {
        return this.nodeBuilder == null;
    }

    /**
     * The default procedure to handle the statement nodes. These nodes should be handled explicitly.
     *
     * @param statementNode the statement node
     */
    private void handleDefaultStatementNode(NonTerminalNode statementNode) {
        handleExpressionNode(statementNode);
        endNode(statementNode);
    }

    private void handleExpressionNode(NonTerminalNode statementNode) {
        // If there is a type binding pattern node, then default to the variable node
        if (typedBindingPatternNode != null) {
            return;
        }

        startNode(NodeKind.EXPRESSION, statementNode)
                .properties().statement(statementNode);
    }

    private void analyzeBlock(BlockStatementNode blockStatement, Branch.Builder branchBuilder) {
        for (Node statementOrComment : blockStatement.statementsWithComments()) {
            statementOrComment.accept(this);
            branchBuilder.node(buildNode());
        }
    }

    @Override
    protected void visitSyntaxNode(Node node) {
        // SKip visiting the child node of non-overridden nodes
    }

    private void genCommentNode(CommentMetadata comment) {
        startNode(NodeKind.COMMENT)
                .metadata().description(comment.comment()).stepOut()
                .properties().comment(comment.comment());
        nodeBuilder.codedata()
                .lineRange(comment.position)
                .sourceCode(comment.comment());
        endNode();
    }

    private String getIcon(String name) {
        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.FUNCTION) {
                continue;
            }
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            if (functionSymbol.nameEquals(name)) {
                for (AnnotationAttachmentSymbol annotAttachment : functionSymbol.annotAttachments()) {
                    if (annotAttachment.typeDescriptor().getName().orElseThrow().equals("display")) {
                        Optional<ConstantValue> optAttachmentValue = annotAttachment.attachmentValue();
                        if (optAttachmentValue.isEmpty()) {
                            throw new IllegalStateException("Annotation attachment value not found");
                        }
                        ConstantValue attachmentValue = optAttachmentValue.get();
                        if (attachmentValue.valueType().typeKind() != TypeDescKind.RECORD) {
                            throw new IllegalStateException("Annotation attachment value is not a record");
                        }
                        HashMap<?, ?> valueMap = (HashMap<?, ?>) attachmentValue.value();
                        if (valueMap.get("iconPath") == null) {
                            throw new IllegalStateException("Icon path not found in the annotation attachment value");
                        }
                        return valueMap.get("iconPath").toString();
                    }
                }
            }
        }
        return "";
    }

    private String getToolDescription(String toolName) {
        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.FUNCTION) {
                continue;
            }
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            if (!functionSymbol.getName().orElseThrow().equals(toolName)) {
                continue;
            }
            Optional<Documentation> optDoc = functionSymbol.documentation();
            if (optDoc.isEmpty()) {
                break;
            }
            Optional<String> optDescription = optDoc.get().description();
            if (optDescription.isEmpty()) {
                break;
            }
            return optDescription.get();
        }

        return "";
    }

    private ImplicitNewExpressionNode getNewExpr(ExpressionNode expressionNode) {
        NonTerminalNode expr = expressionNode;
        if (expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION) {
            expr = ((CheckExpressionNode) expr).expression();
        }
        if (expr.kind() == SyntaxKind.IMPLICIT_NEW_EXPRESSION) {
            return (ImplicitNewExpressionNode) expr;
        }
        throw new IllegalStateException("Implicit new expression not found");
    }

    // Check whether a type symbol is subType of `RawTemplate`
    private boolean isSubTypeOfRawTemplate(TypeSymbol typeSymbol) {
        // TODO: Once https://github.com/ballerina-platform/ballerina-lang/pull/43871 is merged,
        //  we can use `typeSymbol.subtypeOf(semanticModel.types().RAW_TEMPLATE)` to check the subtyping
        TypeDefinitionSymbol rawTypeDefSymbol = (TypeDefinitionSymbol)
                semanticModel.types().getTypeByName("ballerina", "lang.object", "0.0.0", "RawTemplate").get();

        TypeSymbol rawTemplateTypeDesc = rawTypeDefSymbol.typeDescriptor();
        return typeSymbol.subtypeOf(rawTemplateTypeDesc);
    }

    public List<FlowNode> getFlowNodes() {
        return flowNodeList;
    }

    private record CommentMetadata(String comment, LineRange position) {

    }

    private record ToolData(String name, String path, String description, String type) {

    }

    private record ModelData(String name, String path, String type) {

    }

    // TODO: Update data based on requirements
    private record MemoryManagerData(String type, String size) {

    }

    private String removeLeadingAndTrailingMinutiae(Node node, CommentProperty commentProperty) {
        String sourceCode = node.toSourceCode().strip();
        String leadingMinutiae = node.leadingMinutiae().toString();
        if (leadingMinutiae.contains("//")) {
            commentProperty.setLeadingComment(leadingMinutiae.substring(leadingMinutiae.indexOf("//")));
        }
        sourceCode = sourceCode.replace(leadingMinutiae.strip(), "");

        String trailingMinutiae = node.trailingMinutiae().toString();
        if (trailingMinutiae.contains("//")) {
            commentProperty.setTrailingComment(trailingMinutiae.substring(trailingMinutiae.indexOf("//")));
        }
        sourceCode = sourceCode.replace(trailingMinutiae.strip(), "");
        return sourceCode.strip();
    }

    private static String getPathString(NodeList<Node> nodes) {
        return nodes.stream()
                .map(node -> node.toString().trim())
                .collect(Collectors.joining());
    }

    private static boolean hasQualifier(NodeList<Token> qualifierList, SyntaxKind kind) {
        return qualifierList.stream().anyMatch(qualifier -> qualifier.kind() == kind);
    }

    private boolean isAgent(ServiceDeclarationNode serviceDeclarationNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceDeclarationNode.expressions();
        if (expressions.isEmpty()) {
            return false;
        }

        ExpressionNode listenerExpression = expressions.get(0);
        Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(listenerExpression);
        if (typeSymbol.isEmpty()) {
            return false;
        }

        TypeSymbol listenerTypeSymbol = getListenerTypeSymbol(typeSymbol.get());
        if (listenerTypeSymbol == null) {
            return false;
        }

        Optional<ModuleSymbol> module = listenerTypeSymbol.getModule();
        return module.isPresent() && AI_AGENT.equals(module.get().id().moduleName());
    }

    private TypeSymbol getListenerTypeSymbol(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            return unionTypeSymbol.memberTypeDescriptors().stream()
                    .filter(member -> !member.subtypeOf(semanticModel.types().ERROR))
                    .findFirst().orElse(null);
        }
        return typeSymbol;
    }

    /**
     * Represents the function kind to display in the flow model.
     *
     * @since 1.0.1
     */
    public enum FunctionKind {
        FUNCTION("Function"),
        REMOTE_FUNCTION("Remote Function"),
        RESOURCE("Resource"),
        AI_CHAT_AGENT("AI Chat Agent");

        private final String value;

        FunctionKind(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
