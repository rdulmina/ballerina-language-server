/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.debugadapter.completion.util;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.debugadapter.SuspendedContext;
import org.ballerinalang.debugadapter.completion.FunctionCompletionItemBuilder;
import org.ballerinalang.debugadapter.completion.context.CompletionContext;
import org.ballerinalang.debugadapter.evaluation.DebugExpressionCompiler;
import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;
import org.eclipse.lsp4j.debug.CompletionsArguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.api.symbols.SymbolKind.FUNCTION;
import static io.ballerina.compiler.api.symbols.SymbolKind.METHOD;

/**
 * Common utility methods for debug completions operation.
 *
 * @since 1.0.0
 */
public final class CompletionUtil {

    private static final String PARENTHESIS = "()";
    private static final List<String> triggerCharacters = Arrays.asList(".", ">");

    private CompletionUtil() {
    }

    /**
     * Get the updated bal file content which includes debug console expression.
     *
     * @param completionContext debug completion context
     * @param args              debug completions arguments
     * @param node              non terminal node
     * @param lineNumber        debug breakpoint line number
     * @return updated file content
     */
    public static String getUpdatedBalFileContent(CompletionContext completionContext, CompletionsArguments args,
                                                  NonTerminalNode node, int lineNumber) {
        SuspendedContext suspendedContext = completionContext.getSuspendedContext();
        TextDocument textDocument = suspendedContext.getDocument().textDocument();
        List<String> lines = Arrays.asList(textDocument.toString().split(System.lineSeparator()));

        if (node.kind() == SyntaxKind.FUNCTION_BODY_BLOCK) {
            int nodeStartLine = node.lineRange().startLine().line();
            int nodeEndLine = node.lineRange().endLine().line();

            if (nodeStartLine == lineNumber - 1) {
                String lineContent = lines.get(nodeStartLine);
                lineContent = lineContent + System.lineSeparator() + args.getText();
                lines.set(nodeStartLine, lineContent);
            } else if (nodeEndLine == lineNumber - 1) {
                String lineContent = lines.get(nodeEndLine);
                int startLineOffSet = node.lineRange().endLine().offset() - 1;
                lineContent = lineContent.substring(0, startLineOffSet) + args.getText() + System.lineSeparator()
                        + String.join("", Collections.nCopies(startLineOffSet, " "))
                        + lineContent.substring(startLineOffSet);
                lines.set(nodeEndLine, lineContent);
            }
        } else {
            int nodeStartLine = node.lineRange().startLine().line();
            String lineContent = lines.get(nodeStartLine);
            int startLineOffSet = node.lineRange().startLine().offset();
            lineContent = lineContent.substring(0, startLineOffSet) + args.getText() + System.lineSeparator()
                    + String.join("", Collections.nCopies(startLineOffSet, " "))
                    + lineContent.substring(startLineOffSet);
            lines.set(nodeStartLine, lineContent);
        }

        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line).append(System.lineSeparator());
        }
        return result.toString();
    }

    /**
     * Get the nearest matching resolver node.
     *
     * @param node node to evaluate
     * @return nearest matching resolver node
     */
    public static Optional<Node> getResolverNode(NonTerminalNode node) {
        if (node == null || node.kind() == SyntaxKind.MODULE_PART) {
            return Optional.empty();
        } else if (node.kind() == SyntaxKind.FIELD_ACCESS || node.kind() == SyntaxKind.REMOTE_METHOD_CALL_ACTION
                || node.kind() == SyntaxKind.ASYNC_SEND_ACTION) {
            return Optional.of(node);
        }
        return getResolverNode(node.parent());
    }

    /**
     * Get the visible symbol completion items.
     *
     * @param completionContext debug completion context
     * @return visible symbol completion item array
     */
    public static CompletionItem[] getVisibleSymbolCompletions(CompletionContext completionContext) {
        SuspendedContext suspendedContext = completionContext.getSuspendedContext();
        DebugExpressionCompiler debugCompiler = suspendedContext.getDebugCompiler();
        SemanticModel semanticContext = debugCompiler.getSemanticInfo();
        List<Symbol> symbolList = semanticContext.visibleSymbols(
                suspendedContext.getDocument(), LinePosition.from(suspendedContext.getLineNumber() - 1, 0));
        return getCompletions(symbolList);
    }

    /**
     * Get the visible symbol completion items.
     *
     * @param symbolList visible symbol list
     * @return visible symbol completion item array
     */
    public static CompletionItem[] getCompletions(List<Symbol> symbolList) {
        List<CompletionItem> completionItems = new ArrayList<>();

        for (Symbol symbol : symbolList) {
            CompletionItem completionItem = new CompletionItem();
            if (symbol.getName().isPresent()
                    && (symbol.kind().name().equals(CompletionItemType.METHOD.toString())
                    || symbol.kind().name().equals(CompletionItemType.FUNCTION.toString()))) {
                completionItem.setLabel(symbol.getName().get() + PARENTHESIS);
                completionItem.setText(symbol.getName().get() + PARENTHESIS);
            } else {
                completionItem.setLabel(symbol.getName().get());
                completionItem.setText(symbol.getName().get());
            }
            completionItem.setType(getCompletionItemType(symbol));
            completionItems.add(completionItem);
        }
        return completionItems.toArray(new CompletionItem[0]);
    }

    /**
     * Populate the completion item list by considering the.
     *
     * @param scopeEntries list of symbol information
     * @param ctx          debug completion context
     * @return {@link List}     list of completion items
     */
    public static List<CompletionItem> getCompletionItemList(List<? extends Symbol> scopeEntries,
                                                             CompletionContext ctx) {
        List<Symbol> processedSymbols = new ArrayList<>();
        List<CompletionItem> completionItems = new ArrayList<>();
        scopeEntries.forEach(symbol -> {
            if (processedSymbols.contains(symbol)) {
                return;
            }
            if (symbol.kind() == FUNCTION || symbol.kind() == METHOD) {
                completionItems.addAll(populateBallerinaFunctionCompletionItems(symbol, ctx));
            }
            processedSymbols.add(symbol);
        });
        return completionItems;
    }

    /**
     * Populate the Ballerina Function Completion Items.
     *
     * @param symbol symbol Entry
     * @return completion item
     */
    private static List<CompletionItem> populateBallerinaFunctionCompletionItems(Symbol symbol,
                                                                                 CompletionContext context) {

        List<CompletionItem> completionItems = new ArrayList<>();
        if (symbol.kind() != SymbolKind.FUNCTION && symbol.kind() != SymbolKind.METHOD) {
            return completionItems;
        }
        CompletionItem completionItem = FunctionCompletionItemBuilder.build((FunctionSymbol) symbol, context);
        completionItems.add(completionItem);
        return completionItems;
    }

    private static CompletionItemType getCompletionItemType(Symbol symbol) {
        return switch (symbol.kind()) {
            case MODULE -> CompletionItemType.MODULE;
            case FUNCTION -> CompletionItemType.FUNCTION;
            case METHOD,
                 RESOURCE_METHOD -> CompletionItemType.METHOD;
            case VARIABLE -> CompletionItemType.VARIABLE;
            case CLASS -> CompletionItemType.CLASS;
            case RECORD_FIELD,
                 OBJECT_FIELD,
                 CLASS_FIELD -> CompletionItemType.FIELD;
            case ENUM -> CompletionItemType.ENUM;
            default -> null;
        };
    }

    /**
     * Get non terminal node at breakpoint.
     *
     * @param source     bal source file
     * @param sourcePath bal source file path
     * @param lineNumber breakpoint line number
     * @return non terminal node at breakpoint
     */
    public static NonTerminalNode getNonTerminalNode(CompletionContext completionContext, String source,
                                                     String sourcePath, int lineNumber) {
        return getNonTerminalNode(completionContext, source, sourcePath, null, lineNumber, 0);
    }

    /**
     * Get modified non terminal node at breakpoint.
     *
     * @param source          bal source file
     * @param sourcePath      bal source file path
     * @param nonTerminalNode non terminal node
     * @param lineNumber      breakpoint line number
     * @param column          debug expression column number
     * @return non terminal node at breakpoint
     */
    public static NonTerminalNode getNonTerminalNode(CompletionContext completionContext,
                                                     String source, String sourcePath, NonTerminalNode nonTerminalNode,
                                                     int lineNumber, int column) {
        TextDocument document = TextDocuments.from(source);
        SyntaxTree syntaxTree = SyntaxTree.from(document, sourcePath);

        int textPosition = getTextPosition(nonTerminalNode, document, lineNumber, column - 1);
        completionContext.setCursorPositionInTree(textPosition);
        TextRange range = TextRange.from(textPosition, 0);
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(range);
    }

    private static int getTextPosition(NonTerminalNode nonTerminalNode, TextDocument textDocument, int lineNumber,
                                       int offset) {
        int textPosition = 0;

        if (nonTerminalNode == null) {
            return textDocument.textPositionFrom(LinePosition.from(lineNumber, offset));
        } else if (nonTerminalNode.kind() == SyntaxKind.FUNCTION_BODY_BLOCK) {
            if (nonTerminalNode.lineRange().startLine().line() == lineNumber - 1) {
                textPosition = textDocument.textPositionFrom(LinePosition.from(
                        nonTerminalNode.lineRange().startLine().line() + 1, offset));
            } else if (nonTerminalNode.lineRange().endLine().line() == lineNumber - 1) {
                textPosition = textDocument.textPositionFrom(LinePosition.from(
                        nonTerminalNode.lineRange().endLine().line(), offset));
            }
        } else {
            textPosition = textDocument.textPositionFrom(
                    LinePosition.from(nonTerminalNode.lineRange().startLine().line(),
                            nonTerminalNode.lineRange().startLine().offset() + offset));
        }
        return textPosition;
    }

    public static boolean triggerCharactersFound(String expression) {
        return Arrays.stream(expression.split("")).anyMatch(triggerCharacters::contains);
    }

    public static List<String> getTriggerCharacters() {
        return triggerCharacters;
    }

    /**
     * Get non terminal node with injected expression.
     *
     * @param completionContext debug completion context
     * @param args              debug completions arguments
     * @param sourcePath        source path
     * @param lineNumber        debug hit line
     * @return non terminal node with injected expression
     */
    public static NonTerminalNode getInjectedExpressionNode(CompletionContext completionContext,
                                                            CompletionsArguments args, String sourcePath,
                                                            int lineNumber) {
        // TODO: Getting injected expression node logic can be improved by building the syntax tree with text edits
        //  after fixing the issue https://github.com/ballerina-platform/ballerina-lang/issues/24058

        // Identify the non terminal node at breakpoint before injecting the debug console expression.
        SuspendedContext suspendedContext = completionContext.getSuspendedContext();
        String source = suspendedContext.getDocument().textDocument().toString();
        NonTerminalNode nonTerminalNode = getNonTerminalNode(completionContext, source, sourcePath, lineNumber);

        // Debug console expressions are injected at the beginning of StatementNode, except FunctionBodyBlockNode.
        // If the non terminal node is not an instance of StatementNode,
        // traverse/visit to its parent until get the instance of StatementNode.
        while (!(nonTerminalNode instanceof StatementNode)) {
            if (nonTerminalNode.kind() == SyntaxKind.FUNCTION_BODY_BLOCK) {
                break;
            }
            nonTerminalNode = nonTerminalNode.parent();
        }

        // Inject the debug console expression.
        String updatedSource = getUpdatedBalFileContent(completionContext, args, nonTerminalNode, lineNumber);

        return getNonTerminalNode(completionContext, updatedSource, sourcePath, nonTerminalNode, lineNumber,
                args.getColumn());
    }
}
