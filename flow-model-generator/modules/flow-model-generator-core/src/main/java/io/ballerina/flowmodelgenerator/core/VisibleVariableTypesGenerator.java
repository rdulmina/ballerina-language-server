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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates types of the visible variable types for the given cursor position.
 *
 * @since 1.0.0
 */
public class VisibleVariableTypesGenerator {

    private final SemanticModel semanticModel;
    private final Document document;
    private final LinePosition position;
    private final Gson gson;
    private final Map<String, List<Category.Variable>> categoryMap;
    private static final List<String> CATEGORY_ORDER = Arrays.asList(
            Category.LOCAL_CATEGORY,
            Category.MODULE_CATEGORY,
            Category.CONFIGURABLE_CATEGORY,
            Category.PARAMETER_CATEGORY,
            Category.PATH_PARAMETER_CATEGORY
    );

    public VisibleVariableTypesGenerator(SemanticModel semanticModel, Document document, LinePosition position) {
        this.semanticModel = semanticModel;
        this.document = document;
        this.position = position;
        this.gson = new Gson();
        this.categoryMap = new HashMap<>();
    }

    public JsonArray getVisibleVariableTypes() {
        Optional<LineRange> functionLineRange = findFunctionLineRange();

        List<Symbol> symbols = semanticModel.visibleSymbols(document, position);
        for (Symbol symbol : symbols) {
            if (symbol.kind() == SymbolKind.VARIABLE) {
                VariableSymbol variableSymbol = (VariableSymbol) symbol;
                String name = variableSymbol.getName().orElse("");
                Type type = fromTypeSymbol(variableSymbol.typeDescriptor());

                // Skip the object types
                TypeSymbol typeSymbol = variableSymbol.typeDescriptor();
                TypeSymbol effectiveTypeSymbol;
                if (typeSymbol instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
                    effectiveTypeSymbol = typeReferenceTypeSymbol.typeDescriptor();
                } else {
                    effectiveTypeSymbol = typeSymbol;
                }
                if (effectiveTypeSymbol instanceof ObjectTypeSymbol) {
                    continue;
                }

                if (variableSymbol.qualifiers().contains(Qualifier.CONFIGURABLE)) {
                    addCategoryValue(Category.CONFIGURABLE_CATEGORY, name, type);
                } else if (functionLineRange.isPresent() &&
                        isInFunctionRange(variableSymbol, functionLineRange.get())) {
                    addCategoryValue(Category.LOCAL_CATEGORY, name, type);
                } else {
                    addCategoryValue(Category.MODULE_CATEGORY, name, type);
                }
            } else if (symbol.kind() == SymbolKind.PARAMETER) {
                String name = symbol.getName().orElse("");
                Type type = fromTypeSymbol(((ParameterSymbol) symbol).typeDescriptor());
                addCategoryValue(Category.PARAMETER_CATEGORY, name, type);
            } else if (symbol.kind() == SymbolKind.PATH_PARAMETER) {
                String name = symbol.getName().orElse("");
                PathParameterSymbol pathParameterSymbol = (PathParameterSymbol) symbol;
                Type type = fromTypeSymbol(pathParameterSymbol.typeDescriptor());
                type.isRestType = pathParameterSymbol.pathSegmentKind() == PathSegment.Kind.PATH_REST_PARAMETER;
                addCategoryValue(Category.PATH_PARAMETER_CATEGORY, name, type);
            }
        }

        // Add categories in the defined order if they exist in the map
        List<Category> categories = new ArrayList<>();
        for (String categoryName : CATEGORY_ORDER) {
            List<Category.Variable> variables = categoryMap.get(categoryName);
            if (variables != null) {
                Collections.sort(variables);
                categories.add(new Category(categoryName, variables));
            }
        }

        return gson.toJsonTree(categories).getAsJsonArray();
    }

    private Type fromTypeSymbol(TypeSymbol typeSymbol) {
        String typeName = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false,
                ModuleInfo.from(document.module().descriptor()));
        Type newType = new Type();
        newType.typeName = getTypeName(typeSymbol);
        newType.name = typeName;
        return newType;
    }

    private String getTypeName(TypeSymbol typeSymbol) {
        if (typeSymbol instanceof RecordTypeSymbol) {
            return "record";
        } else if (typeSymbol instanceof ArrayTypeSymbol) {
            return "array";
        } else if (typeSymbol instanceof MapTypeSymbol) {
            return "map";
        } else if (typeSymbol instanceof TableTypeSymbol) {
            return "table";
        } else if (typeSymbol instanceof UnionTypeSymbol) {
            return "union";
        } else if (typeSymbol instanceof ErrorTypeSymbol) {
            return "error";
        } else if (typeSymbol instanceof IntersectionTypeSymbol) {
            return "intersection";
        } else if (typeSymbol instanceof StreamTypeSymbol) {
            return "stream";
        } else if (typeSymbol instanceof ObjectTypeSymbol) {
            return "object";
        } else if (typeSymbol instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
            if (typeReferenceTypeSymbol.definition().kind().equals(SymbolKind.ENUM)) {
                return "enum";
            }
            return getTypeName(typeReferenceTypeSymbol.typeDescriptor());
        }
        String typeName = typeSymbol.signature();
        if (typeName.startsWith("\"") && typeName.endsWith("\"")) {
            typeName = typeName.substring(1, typeName.length() - 1);
        }
        return typeName;
    }

    /**
     * Adds a variable to the specified category.
     *
     * @param categoryName the name of the category
     * @param name         the name of the variable
     * @param type         the type of the variable
     */
    private void addCategoryValue(String categoryName, String name, Type type) {
        categoryMap.computeIfAbsent(categoryName, k -> new ArrayList<>())
                .add(new Category.Variable(name, type));
    }

    private boolean isInFunctionRange(VariableSymbol variableSymbol, LineRange functionLineRange) {
        return variableSymbol.getLocation().map(loc -> {
            LineRange variableLineRange = loc.lineRange();
            if (!variableLineRange.fileName().equals(functionLineRange.fileName())) {
                return false;
            }
            return PositionUtil.isWithinLineRange(variableLineRange, functionLineRange);
        }).orElse(false);
    }

    private Optional<LineRange> findFunctionLineRange() {
        ModulePartNode rootNode = document.syntaxTree().rootNode();
        NonTerminalNode parent =
                rootNode.findNode(TextRange.from(document.textDocument().textPositionFrom(position), 0));
        while (parent != null && notDefinitionKind(parent.kind())) {
            parent = parent.parent();
        }
        return parent == null ? Optional.empty() : Optional.of(parent.lineRange());
    }

    private static boolean notDefinitionKind(SyntaxKind kind) {
        return kind != SyntaxKind.FUNCTION_DEFINITION && kind != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
    }

    /**
     * Represents a category of variables.
     *
     * @param name  the name of the category
     * @param types the list of variables in the category
     * @since 1.0.0
     */
    private record Category(String name, List<Variable> types) {

        public static final String LOCAL_CATEGORY = "Local Variables";
        public static final String MODULE_CATEGORY = "Module Variables";
        public static final String CONFIGURABLE_CATEGORY = "Configurable Variables";
        public static final String PARAMETER_CATEGORY = "Parameters";
        public static final String PATH_PARAMETER_CATEGORY = "Path Parameters";

        public record Variable(String name, Type type) implements Comparable<Variable> {

            @Override
            public int compareTo(Variable o) {
                return this.name.compareTo(o.name);
            }
        }

    }

}
