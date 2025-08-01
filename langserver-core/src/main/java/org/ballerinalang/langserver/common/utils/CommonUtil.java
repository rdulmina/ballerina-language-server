/*
 * Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
package org.ballerinalang.langserver.common.utils;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.FunctionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxInfo;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Package;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.apache.commons.lang3.SystemUtils;
import org.ballerinalang.langserver.LSPackageLoader;
import org.ballerinalang.langserver.commons.BallerinaCompletionContext;
import org.ballerinalang.langserver.commons.CodeActionContext;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.completions.SymbolCompletionItem;
import org.ballerinalang.langserver.completions.util.ItemResolverConstants;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.wso2.ballerinalang.compiler.util.Names;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static io.ballerina.compiler.api.symbols.SymbolKind.PARAMETER;
import static org.ballerinalang.langserver.common.utils.CommonKeys.SEMI_COLON_SYMBOL_KEY;
import static org.ballerinalang.langserver.common.utils.CommonKeys.SLASH_KEYWORD_KEY;

/**
 * Common utils to be reused in language server implementation.
 */
public final class CommonUtil {

    public static final String MD_LINE_SEPARATOR = "  " + System.lineSeparator();

    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static final String FILE_SEPARATOR = File.separator;

    public static final Pattern MD_NEW_LINE_PATTERN = Pattern.compile("\\s\\s\\r\\n?|\\s\\s\\n|\\r\\n?|\\n");

    public static final String BALLERINA_HOME;

    public static final boolean COMPILE_OFFLINE;

    public static final String BALLERINA_CMD;

    public static final String URI_SCHEME_BALA = "bala";
    public static final String URI_SCHEME_EXPR = "expr";
    public static final String URI_SCHEME_FILE = "file";
    public static final String LANGUAGE_ID_BALLERINA = "ballerina";
    public static final String LANGUAGE_ID_TOML = "toml";

    public static final String MARKDOWN_MARKUP_KIND = "markdown";

    public static final String BALLERINA_ORG_NAME = "ballerina";

    public static final String SDK_VERSION = System.getProperty("ballerina.version");

    public static final String EXPR_SCHEME = "expr";

    //lang.array, regexp, lang.value, lang.runtime, jballerina.java are not pre-declared.
    public static final List<String> PRE_DECLARED_LANG_LIBS = Arrays.asList("lang.boolean", "lang.decimal",
            "lang.error", "lang.float", "lang.function", "lang.future", "lang.int", "lang.map", "lang.object",
            "lang.stream", "lang.string", "lang.table", "lang.transaction", "lang.typedesc", "lang.xml",
            "lang.natural");

    public static final List<String> BALLERINA_KEYWORDS = SyntaxInfo.keywords();

    public static final Set<SyntaxKind> QUALIFIER_KINDS = Set.of(SyntaxKind.SERVICE_KEYWORD,
            SyntaxKind.CLIENT_KEYWORD, SyntaxKind.ISOLATED_KEYWORD, SyntaxKind.TRANSACTIONAL_KEYWORD,
            SyntaxKind.PUBLIC_KEYWORD, SyntaxKind.PRIVATE_KEYWORD, SyntaxKind.CONFIGURABLE_KEYWORD);

    private static final Pattern TYPE_NAME_DECOMPOSE_PATTERN = Pattern.compile("([\\w_.]*)/([\\w._]*):([\\w.-]*)");
    private static final String UNRESOLVED_MODULE_CODE = "BCE2003";

    static {
        BALLERINA_HOME = System.getProperty("ballerina.home");
        String onlineCompilation = System.getProperty("ls.compilation.online");
        COMPILE_OFFLINE = !Boolean.parseBoolean(onlineCompilation);
        BALLERINA_CMD = BALLERINA_HOME + File.separator + "bin" + File.separator + "bal" +
                (SystemUtils.IS_OS_WINDOWS ? ".bat" : "");
    }

    private CommonUtil() {
    }

    /**
     * Get the text edit for an auto import statement.
     * Here we do not check whether the package is not already imported or a predeclared lang-lib, Particular
     * check should be done before usage
     *
     * @param orgName package org name
     * @param pkgName package name
     * @param context Language server context
     * @return {@link List}     List of Text Edits to apply
     */
    public static List<TextEdit> getAutoImportTextEdits(@Nonnull String orgName, String pkgName,
                                                        DocumentServiceContext context) {
        Map<ImportDeclarationNode, ModuleSymbol> currentDocImports = context.currentDocImportsMap();
        Optional<ImportDeclarationNode> last = CommonUtil.getLastItem(new ArrayList<>(currentDocImports.keySet()));
        int endLine = last.map(node -> node.lineRange().endLine().line()).orElse(0);
        Position start = new Position(endLine, 0);
        String importStatement = ItemResolverConstants.IMPORT + " "
                + (!orgName.isEmpty() ? orgName + SLASH_KEYWORD_KEY : orgName)
                + pkgName + SEMI_COLON_SYMBOL_KEY
                + CommonUtil.LINE_SEPARATOR;

        return Collections.singletonList(new TextEdit(new Range(start, start), importStatement));
    }

    /**
     * Get the text edit for an auto import statement.
     * Here we do not check whether the package is not already imported or a predeclared lang-lib, Particular
     * check should be done before usage
     *
     * @param orgName package org name
     * @param pkgName package name
     * @param alias   import alias
     * @param context Language server context
     * @return {@link List}     List of Text Edits to apply
     */
    public static List<TextEdit> getAutoImportTextEdits(@Nonnull String orgName, String pkgName, String alias,
                                                        DocumentServiceContext context) {

        StringBuilder builder = new StringBuilder(ItemResolverConstants.IMPORT + " "
                + (!orgName.isEmpty() ? orgName + SLASH_KEYWORD_KEY : orgName)
                + pkgName);
        if (!alias.isEmpty()) {
            builder.append(" as ").append(alias);
        }
        builder.append(SEMI_COLON_SYMBOL_KEY).append(CommonUtil.LINE_SEPARATOR);

        Position insertPos = getImportPosition(context);
        return Collections.singletonList(new TextEdit(new Range(insertPos, insertPos), builder.toString()));
    }

    public static Position getImportPosition(DocumentServiceContext context) {
        // Calculate initial import insertion line
        Optional<SyntaxTree> syntaxTree = context.currentSyntaxTree();
        ModulePartNode modulePartNode = syntaxTree.orElseThrow().rootNode();
        NodeList<ImportDeclarationNode> imports = modulePartNode.imports();
        // If there is already an import, add the new import after the last import
        if (!imports.isEmpty()) {
            ImportDeclarationNode lastImport = imports.get(imports.size() - 1);
            return new Position(lastImport.lineRange().endLine().line() + 1, 0);
        }

        // If the module part has no children, add the import at the beginning of the file
        if (modulePartNode.members().isEmpty()) {
            return new Position(0, 0);
        }

        Position insertPosition = new Position(0, 0);
        for (Minutiae minutiae : modulePartNode.leadingMinutiae()) {
            if (minutiae.kind() == SyntaxKind.END_OF_LINE_MINUTIAE
                    && minutiae.lineRange().startLine().offset() == 0) {
                // If we find a new line character with offset 0 (a blank line), add the import after that
                // And no further processing is required
                insertPosition = new Position(minutiae.lineRange().startLine().line(), 0);
                break;
            }
        }

        return insertPosition;
    }

    /**
     * Get the last item of the List.
     *
     * @param list List to get the Last Item
     * @param <T>  List content Type
     * @return Extracted last Item
     */
    public static <T> Optional<T> getLastItem(List<T> list) {
        return (list.isEmpty()) ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    /**
     * Whether the given module is a langlib module.
     * <pre>
     * public static String generateParameterName(String arg, Set&lt;String&gt; visibleNames) {
     * visibleNames.addAll(BALLERINA_KEYWORDS);
     * String newName = arg.replaceAll(".+[\\:\\.]", "");
     *
     *
     * }
     * </pre>
     * @param moduleID Module ID to evaluate
     * @return {@link Boolean} whether langlib or not
     */
    public static boolean isLangLib(ModuleID moduleID) {
        return isLangLib(moduleID.orgName(), moduleID.moduleName());
    }

    public static boolean isLangLib(String orgName, String moduleName) {
        return orgName.equals("ballerina") && moduleName.startsWith("lang.");
    }

    /**
     * Whether the given module is a langlib module or the Ballerina test module.
     *
     * @param moduleID Module ID to evaluate
     * @return {@link Boolean} <code>true</code> if the given module is either a langlib module or
     * the Ballerina test module. <code>false</code> otherwise.
     */
    public static boolean isLangLibOrLangTest(ModuleID moduleID) {
        String orgName = moduleID.orgName();
        String moduleName = moduleID.moduleName();
        return orgName.equals("ballerina") && (moduleName.startsWith("lang.") ||  moduleName.equals("test"));
    }

    /**
     * Escapes the escape characters present in an identifier.
     *
     * @param identifier Identifier
     * @return The identifier with escape characters escaped
     */
    public static String escapeEscapeCharsInIdentifier(String identifier) {
        return identifier.replace("\\", "\\\\");
    }

    /**
     * As per LSP, characters like \ and $ should be escaped when using them as insert text.
     *
     * @param text The text to be processed
     * @return Processed text
     */
    public static String escapeSpecialCharsInInsertText(String text) {
        return text.replace("\\", "\\\\")
                .replace("$", "\\$");
    }

    /**
     * Find node of this range.
     *
     * @param range      {@link Range}
     * @param syntaxTree {@link SyntaxTree}
     * @return {@link NonTerminalNode}
     */
    public static NonTerminalNode findNode(Range range, SyntaxTree syntaxTree) {
        TextDocument textDocument = syntaxTree.textDocument();
        Position rangeStart = range.getStart();
        Position rangeEnd = range.getEnd();
        int start = textDocument.textPositionFrom(LinePosition.from(rangeStart.getLine(), rangeStart.getCharacter()));
        int end = textDocument.textPositionFrom(LinePosition.from(rangeEnd.getLine(), rangeEnd.getCharacter()));
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }

    /**
     * Find node of this symbol.
     *
     * @param symbol     {@link Symbol}
     * @param syntaxTree {@link SyntaxTree}
     * @return {@link NonTerminalNode}
     */
    public static Optional<NonTerminalNode> findNode(Symbol symbol, SyntaxTree syntaxTree) {
        if (symbol.getLocation().isEmpty()) {
            return Optional.empty();
        }

        TextDocument textDocument = syntaxTree.textDocument();
        LineRange symbolRange = symbol.getLocation().get().lineRange();
        int start = textDocument.textPositionFrom(symbolRange.startLine());
        int end = textDocument.textPositionFrom(symbolRange.endLine());
        return Optional.ofNullable(((ModulePartNode) syntaxTree.rootNode())
                .findNode(TextRange.from(start, end - start), true));
    }

    /**
     * Get the raw type of the type descriptor. If the type descriptor is a type reference then return the associated
     * type descriptor.
     *
     * @param typeDescriptor type descriptor to evaluate
     * @return {@link TypeSymbol} extracted type descriptor
     */
    public static TypeSymbol getRawType(TypeSymbol typeDescriptor) {
        if (typeDescriptor.typeKind() == TypeDescKind.INTERSECTION) {
            return getRawType(((IntersectionTypeSymbol) typeDescriptor).effectiveTypeDescriptor());
        }
        if (typeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeDescriptor;
            if (typeRef.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION) {
                return getRawType(((IntersectionTypeSymbol) typeRef.typeDescriptor()).effectiveTypeDescriptor());
            }
            TypeSymbol rawType = typeRef.typeDescriptor();
            if (rawType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                return getRawType(rawType);
            }
            return rawType;
        }
        return typeDescriptor;
    }

    /**
     * Check whether the given type is an error type or union of error types.
     *
     * @param type type descriptor to evaluate
     * @return {@link Boolean} is error type or union or error types
     */
    public static boolean isErrorOrUnionOfErrors(TypeSymbol type) {
        TypeDescKind kind = type.typeKind();
        if (kind == TypeDescKind.ERROR) {
            return true;
        }
        if (kind == TypeDescKind.TYPE_REFERENCE) {
            TypeSymbol rawType = getRawType(type);
            if (rawType.typeKind() == TypeDescKind.UNION) {
                return ((UnionTypeSymbol) rawType).memberTypeDescriptors().stream()
                        .allMatch(CommonUtil::isErrorOrUnionOfErrors);
            }
            return isErrorOrUnionOfErrors(rawType);
        }
        return false;
    }
    
    private static void getErrorTypes(TypeSymbol type, TypeSymbol typeRef, List<TypeSymbol> errorTypes) {
        TypeDescKind kind = type.typeKind();
        if (kind == TypeDescKind.ERROR) {
            errorTypes.add(Objects.requireNonNullElse(typeRef, type));
        } else if (kind == TypeDescKind.TYPE_REFERENCE) {
            TypeSymbol rawType = getRawType(type);
            if (rawType.typeKind() == TypeDescKind.UNION) {
                UnionTypeSymbol unionRawType = (UnionTypeSymbol) rawType;
                if (unionRawType.memberTypeDescriptors().stream().allMatch(CommonUtil::isErrorOrUnionOfErrors)) { 
                    errorTypes.add(type);    
                } else {
                    for (TypeSymbol memberType : unionRawType.userSpecifiedMemberTypes()) {
                        getErrorTypes(memberType, memberType, errorTypes);
                    }
                }
            } else {
                getErrorTypes(rawType, type, errorTypes);
            }
        }
    }

    /**
     * Extract member error types from the union type.
     *
     * @param unionType union type descriptor to evaluate
     * @return {@link List<TypeSymbol>} member error types
     */
    public static List<TypeSymbol> extractErrorTypesFromUnion(UnionTypeSymbol unionType) {
        List<TypeSymbol> exactErrorTypes = new ArrayList<>();
        for (TypeSymbol memType : unionType.userSpecifiedMemberTypes()) {
            getErrorTypes(memType, null, exactErrorTypes);
        }
        return exactErrorTypes;
    }

    /**
     * Check if the provided union type is a union of members of provided type desc kind.
     *
     * @param typeSymbol   Union type symbol
     * @param typeDescKind Type desc kind
     * @return true if provided union contains members of provided type desc kind
     */
    public static boolean isUnionOfType(TypeSymbol typeSymbol, TypeDescKind typeDescKind) {
        return typeSymbol.typeKind() == TypeDescKind.UNION &&
                ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors().stream()
                        .map(CommonUtil::getRawType)
                        .map(TypeSymbol::typeKind)
                        .allMatch(kind -> kind == typeDescKind);
    }

    /**
     * Get the completion item label for a given package.
     *
     * @param module {@link Package} module info to evaluate
     * @return {@link String} label computed
     */
    public static String getPackageLabel(LSPackageLoader.ModuleInfo module) {
        String orgName = "";
        if (!module.packageOrg().isEmpty() && !module.packageOrg().equals(Names.ANON_ORG.getValue())) {
            orgName = module.packageOrg() + "/";
        }

        return orgName + module.packageName();
    }

    /**
     * Given a signature, this method will remove the version information from the signature.
     *
     * @param context   Context
     * @param signature Signature to be modified.
     * @return Modified signature
     */
    public static String getModifiedSignature(DocumentServiceContext context, String signature) {
        Matcher matcher = TYPE_NAME_DECOMPOSE_PATTERN.matcher(signature);
        while (matcher.find()) {
            String orgName = matcher.group(1);
            String moduleName = matcher.group(2);
            String matchedString = matcher.group();
            String modulePrefix = ModuleUtil.getModulePrefix(context, orgName, moduleName);
            String replaceText = modulePrefix.isEmpty() ? matchedString + Names.VERSION_SEPARATOR : matchedString;
            signature = signature.replace(replaceText, modulePrefix);
        }

        return signature;
    }

    /**
     * Escape a given value.
     *
     * @param value to be escape
     * @return {@link String}
     */
    public static String escapeReservedKeyword(String value) {
        if (SyntaxInfo.isKeyword(value)) {
            return "'" + value;
        }

        return value;
    }

    /**
     * Get the predicate to filter the variables.
     * These variables include
     * (1) any variable defined
     * (2) Function Parameters
     * (3) Service/ resource path parameters
     *
     * @return {@link Predicate<Symbol>}
     */
    public static Predicate<Symbol> getVariableFilterPredicate() {
        return symbol -> (symbol instanceof VariableSymbol || symbol.kind() == PARAMETER
                || symbol.kind() == SymbolKind.PATH_PARAMETER)
                && !symbol.getName().orElse("").equals(Names.ERROR.getValue());
    }

    /**
     * Check if the cursor is positioned in a lock statement node context.
     *
     * @param context Completion context.
     * @return {@link Boolean} Whether the cursor is in lock statement node context.
     */
    public static Boolean withinLockStatementNode(BallerinaCompletionContext context) {
        NonTerminalNode evalNode = context.getNodeAtCursor();
        do {
            if (evalNode.kind() == SyntaxKind.LOCK_STATEMENT) {
                return true;
            }
            evalNode = evalNode.parent();
        }
        while (evalNode != null);
        return false;
    }

    /**
     * Get the common predicate to filter the types.
     *
     * @return {@link Predicate}
     */
    public static Predicate<Symbol> typesFilter() {
        // Specifically remove the error type, since this is covered with langlib suggestion and type builtin types
        return symbol -> (symbol.kind() == SymbolKind.TYPE_DEFINITION ||
                symbol.kind() == SymbolKind.CLASS || symbol.kind() == SymbolKind.ENUM
                || symbol.kind() == SymbolKind.ENUM_MEMBER || symbol.kind() == SymbolKind.CONSTANT)
                && !Names.ERROR.getValue().equals(symbol.getName().orElse(""));
    }

    /**
     * Provided a node, returns the list of possible qualifiers of that node.
     *
     * @param node node.
     * @return {@link List<Token>} qualifiers list.
     */
    public static List<Token> getQualifiersOfNode(BallerinaCompletionContext context, Node node) {
        List<Token> qualifiers = new ArrayList<>();
        switch (node.kind()) {
            case FUNCTION_TYPE_DESC:
                ((FunctionTypeDescriptorNode) node).qualifierList().stream().forEach(qualifiers::add);
                break;
            case OBJECT_TYPE_DESC:
                ((ObjectTypeDescriptorNode) node).objectTypeQualifiers().stream().forEach(qualifiers::add);
                break;
            case OBJECT_FIELD:
                ObjectFieldNode objectFieldNode = (ObjectFieldNode) node;
                objectFieldNode.visibilityQualifier().ifPresent(qualifiers::add);
                objectFieldNode.qualifierList().stream().forEach(qualifiers::add);
                break;
            case MODULE_VAR_DECL:
                ModuleVariableDeclarationNode moduleVar = (ModuleVariableDeclarationNode) node;
                Optional<Token> visibilityQualifier = moduleVar.visibilityQualifier();
                visibilityQualifier.ifPresent(qualifiers::add);
                moduleVar.qualifiers().forEach(qualifiers::add);
                Set<SyntaxKind> qualKinds = qualifiers.stream().map(Node::kind).collect(Collectors.toSet());
                getQualifiersAtCursor(context).stream()
                        .filter(qual -> !qualKinds.contains(qual.kind())).forEach(qualifiers::add);

                //Add leading invalid tokens of type binding pattern if there are no visible qualifiers.
                if (qualifiers.isEmpty()) {
                    moduleVar.typedBindingPattern().leadingInvalidTokens().stream()
                            .filter(token -> QUALIFIER_KINDS.contains(token.kind())).forEach(qualifiers::add);
                }
                break;
            case MODULE_PART:
                List<Token> qualsAtCursor = getQualifiersAtCursor(context);
                context.getNodeAtCursor().leadingInvalidTokens().stream()
                        .filter(token -> QUALIFIER_KINDS.contains(token.kind()))
                        .forEach(qualifiers::add);
                // Avoid duplicating the token at cursor.
                qualsAtCursor.stream()
                        .filter(token -> qualifiers.stream()
                                .noneMatch(qual -> qual.textRange().equals(token.textRange())))
                        .forEach(qualifiers::add);
                return qualifiers;
            default:
        }
        //Qualifiers are identified as invalid tokens by the parser in some cases.
        Set<SyntaxKind> qualKinds = qualifiers.stream().map(Node::kind).collect(Collectors.toSet());
        node.leadingInvalidTokens().stream()
                .filter(token -> QUALIFIER_KINDS.contains(token.kind())
                        && !qualKinds.contains(token.kind())).forEach(qualifiers::add);
        return qualifiers;
    }

    /**
     * Get the qualifiers of the module part context node.
     *
     * @param context completion context.
     * @return {@link List<Token> } the list of qualifiers.
     */
    public static List<Token> getQualifiersAtCursor(BallerinaCompletionContext context) {
        List<Token> qualifiers = new ArrayList<>();
        Token tokenAtCursor = context.getTokenAtCursor();
        if (CommonUtil.QUALIFIER_KINDS.contains(tokenAtCursor.kind())) {
            qualifiers.add(tokenAtCursor);
            return qualifiers;
        }
        List<Minutiae> tokensFromMinutiae = new ArrayList<>();
        context.getTokenAtCursor().leadingMinutiae().forEach(minutiae -> {
            if (minutiae.kind() != SyntaxKind.WHITESPACE_MINUTIAE
                    && minutiae.kind() != SyntaxKind.END_OF_LINE_MINUTIAE) {
                tokensFromMinutiae.add(minutiae);
            }
        });
        if (tokensFromMinutiae.isEmpty()) {
            return qualifiers;
        }
        Minutiae tokenValueAtCursor = tokensFromMinutiae.get(tokensFromMinutiae.size() - 1);
        tokenValueAtCursor.invalidTokenMinutiaeNode().ifPresent(invalidTokenMinutiaeNode -> {
            Token token = invalidTokenMinutiaeNode.invalidToken();
            if (CommonUtil.QUALIFIER_KINDS.contains(token.kind())) {
                qualifiers.add(token);
            }
        });
        return qualifiers;
    }

    /**
     * Check whether the completion item type belongs to the types list passed.
     *
     * @param lsCItem   Completion item
     * @param typesList List of types
     * @return {@link Boolean}
     */
    public static boolean isCompletionItemOfType(LSCompletionItem lsCItem, List<TypeDescKind> typesList) {

        if (lsCItem.getType() != LSCompletionItem.CompletionItemType.SYMBOL) {
            return false;
        }

        Symbol symbol = ((SymbolCompletionItem) lsCItem).getSymbol().orElse(null);
        Optional<TypeSymbol> typeDesc = SymbolUtil.getTypeDescriptor(symbol);

        if (typeDesc.isPresent()) {
            TypeSymbol rawType = CommonUtil.getRawType(typeDesc.get());
            return typesList.contains(rawType.typeKind());
        }
        return false;
    }

    /**
     * Given a node (currentNode) start looking up the parent ladder until the given predicate is satisfied.
     *
     * @param currentNode Node to start looking
     * @param predicate   to be satisfied
     * @return {@link Optional}
     */
    public static Optional<Node> getMatchingNode(Node currentNode, Predicate<Node> predicate) {
        Node evalNode = currentNode;
        while (evalNode != null && !predicate.test(evalNode)) {
            evalNode = evalNode.parent();
        }

        return Optional.ofNullable(evalNode);
    }

    /**
     * Get the list of function arguments from the invokable symbol.
     *
     * @param symbol Invokable symbol to extract the arguments
     * @param ctx    Lang Server Operation context
     * @return {@link List} List of arguments
     */
    public static List<String> getFuncArguments(FunctionSymbol symbol, BallerinaCompletionContext ctx) {
        List<ParameterSymbol> params = CommonUtil.getFunctionParameters(symbol, ctx);
        return params.stream().map(param -> getFunctionParamaterSyntax(param, ctx).orElse("")).toList();
    }

    /**
     * Get the function parameter syntax given the parameter symbol.
     *
     * @param param parameter symbol.
     * @param ctx   Lang Server Operation context
     * @return {@link Optional<String>} Type and name syntax string.
     */
    public static Optional<String> getFunctionParamaterSyntax(ParameterSymbol param, BallerinaCompletionContext ctx) {

        if (param.paramKind() == ParameterKind.REST) {
            ArrayTypeSymbol typeSymbol = (ArrayTypeSymbol) param.typeDescriptor();
            return Optional.of(NameUtil.getModifiedTypeName(ctx, typeSymbol.memberTypeDescriptor())
                    + (param.getName().isEmpty() ? "" : "... "
                    + param.getName().get()));
        }

        if (param.typeDescriptor().typeKind() == TypeDescKind.COMPILATION_ERROR) {
            // Invalid parameters are ignored, but empty string is used to indicate there's a parameter
            return Optional.empty();
        } else {
            return Optional.of(NameUtil.getModifiedTypeName(ctx, param.typeDescriptor()) +
                    (param.getName().isEmpty() ? "" : " " + param.getName().get()));
        }
    }

    /**
     * Get the list of function parameters from the invokable symbol.
     *
     * @param symbol Invokable symbol to extract the parameters
     * @param ctx    Lang Server Operation context
     * @return {@link List<ParameterSymbol> } list of parameter symbols.
     */
    public static List<ParameterSymbol> getFunctionParameters(FunctionSymbol symbol, BallerinaCompletionContext ctx) {
        boolean skipFirstParam = skipFirstParam(ctx, symbol);
        FunctionTypeSymbol functionTypeDesc = symbol.typeDescriptor();
        Optional<ParameterSymbol> restParam = functionTypeDesc.restParam();
        List<ParameterSymbol> parameterDefs = new ArrayList<>();

        if (functionTypeDesc.params().isPresent()) {
            List<ParameterSymbol> params = functionTypeDesc.params().get();
            if (skipFirstParam) {
                if (params.size() > 1) {
                    parameterDefs.addAll(params.subList(1, params.size()));
                }
            } else {
                parameterDefs.addAll(params);
            }
        }
        restParam.ifPresent(parameterDefs::add);
        return parameterDefs;
    }

    /**
     * Whether we skip the first parameter being included as a label in the signature.
     * When showing a lang lib invokable symbol over DOT(invocation) we do not show the first param, but when we
     * showing the invocation over package of the langlib with the COLON we show the first param.
     * <p>
     * When the langlib function is retrieved from the Semantic API, those functions are filtered where the first param
     * type not being same as the langlib type. Hence we need to chek whether the function is from a langlib.
     *
     * @param context        context
     * @param functionSymbol invokable symbol
     * @return {@link Boolean} whether we show the first param or not
     */
    public static boolean skipFirstParam(BallerinaCompletionContext context, FunctionSymbol functionSymbol) {
        NonTerminalNode nodeAtCursor = context.getNodeAtCursor();
        return CommonUtil.isLangLib(functionSymbol.getModule().get().id())
                && nodeAtCursor.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE;
    }

    /**
     * Returns ballerina text edit for a given lsp4j text edit.
     *
     * @param syntaxTree syntax tree
     * @param textEdit   lsp4j text edit
     * @return Ballerina text edit
     */
    public static io.ballerina.tools.text.TextEdit getTextEdit(SyntaxTree syntaxTree,
                                                               org.eclipse.lsp4j.TextEdit textEdit) {
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(PositionUtil.getLinePosition(textEdit.getRange().getStart()));
        int end = textDocument.textPositionFrom(PositionUtil.getLinePosition(textEdit.getRange().getEnd()));
        return io.ballerina.tools.text.TextEdit.from(TextRange.from(start, end - start), textEdit.getNewText());
    }

    /**
     * Get the last appearing qualifier token of a given node.
     *
     * @param context completion context.
     * @param node    node.
     * @return {@link Token} lastQualifier.
     */
    public static Optional<Token> getLastQualifier(BallerinaCompletionContext context, Node node) {
        List<Token> qualifiers = getQualifiersOfNode(context, node);
        if (qualifiers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(qualifiers.get(qualifiers.size() - 1));
    }

    /**
     * Checks if there are multiple diagnostics for the considered code action.
     *
     * @param context               The code action context
     * @param node                  The non-terminal node around which the diagnostics are being checked
     * @param currentDiagnostic     The current diagnostic being evaluated
     * @param diagnostics           The diagnostics relevant to the code action
     * @param prioritizedDiagnostic The prioritized diagnostic that won't being ignored.
     * @return {@code true} if there are multiple diagnostics for the relevant code action.
     */
    public static boolean hasMultipleDiagnostics(CodeActionContext context, NonTerminalNode node,
                                                 Diagnostic currentDiagnostic,
                                                 Set<String> diagnostics,
                                                 String prioritizedDiagnostic) {
        List<Diagnostic> projectDiagnostics = context.diagnostics(context.filePath());
        return projectDiagnostics.size() > 1 &&
                projectDiagnostics.stream().anyMatch(diagnostic -> !currentDiagnostic.equals(diagnostic) &&
                        diagnostics.contains(diagnostic.diagnosticInfo().code()) &&
                        PositionUtil.isWithinLineRange(diagnostic.location().lineRange(), node.lineRange())) &&
                currentDiagnostic.diagnosticInfo().code().equals(prioritizedDiagnostic);
    }

    /**
     * Checks if there are unresolved modules in the given semantic model.
     *
     * @param semanticModel The semantic model to check for unresolved modules.
     * @return {@code true} if there are unresolved modules, {@code false} otherwise.
     */
    public static boolean hasUnresolvedModules(SemanticModel semanticModel) {
        return semanticModel.diagnostics().stream()
                .anyMatch(diagnostic -> UNRESOLVED_MODULE_CODE.equals(diagnostic.diagnosticInfo().code()));
    }

    /**
     * Returns the matching node for a given node.
     *
     * @param nodeAtCursor node
     * @return node
     */
    public static Optional<Node> getMappingContextEvalNode(Node nodeAtCursor) {
        Predicate<Node> predicate = node ->
                node.kind() == SyntaxKind.MAPPING_CONSTRUCTOR
                        || node.parent().kind() == SyntaxKind.MAPPING_CONSTRUCTOR
                        || node.kind() == SyntaxKind.MAPPING_MATCH_PATTERN
                        || node.parent().kind() == SyntaxKind.MAPPING_MATCH_PATTERN
                        || node.kind() == SyntaxKind.SPECIFIC_FIELD
                        || node.kind() == SyntaxKind.COMPUTED_NAME_FIELD;
        return CommonUtil.getMatchingNode(nodeAtCursor, predicate);
    }
}
