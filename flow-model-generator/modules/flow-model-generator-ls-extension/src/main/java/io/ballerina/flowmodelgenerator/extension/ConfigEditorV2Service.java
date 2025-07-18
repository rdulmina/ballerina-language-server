/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableDeleteRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableGetRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableNodeTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableUpdateRequest;
import io.ballerina.flowmodelgenerator.extension.response.AbstractFlowModelResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariableDeleteResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariableNodeTemplateResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariableUpdateResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariablesGetResponse;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDependency;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PlatformLibraryScope;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.TomlType;
import io.ballerina.toml.semantic.ast.TomlArrayValueNode;
import io.ballerina.toml.semantic.ast.TomlKeyValueNode;
import io.ballerina.toml.semantic.ast.TomlNode;
import io.ballerina.toml.semantic.ast.TomlTableNode;
import io.ballerina.toml.semantic.ast.TomlValueNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.flowmodelgenerator.core.model.Property.CONFIG_VALUE_KEY;
import static io.ballerina.flowmodelgenerator.core.model.Property.CONFIG_VAR_DOC_KEY;
import static io.ballerina.flowmodelgenerator.core.model.Property.DEFAULT_VALUE_KEY;
import static io.ballerina.flowmodelgenerator.core.model.Property.VARIABLE_KEY;

/**
 * Provides extended services for viewing and editing Ballerina configuration variables.
 *
 * @since 1.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("configEditorV2")
public class ConfigEditorV2Service implements ExtendedLanguageServerService {

    // File and Path Constants
    private static final String CONFIG_TOML_FILENAME = "Config.toml";

    // Character and Separator Constants
    private static final String FORWARD_SLASH = "/";
    private static final String DOT = ".";
    private static final String HASH_COMMENT_PREFIX = "# ";
    private static final String EMPTY_STRING = "";
    private static final String QUESTION_MARK = "?";
    private static final String COMMA_SPACE = ", ";
    private static final String OPEN_BRACE = "{";
    private static final String CLOSE_BRACE = "}";
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String COLON_SPACE = ": ";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String EQUALS_SIGN_SPACED = " = ";

    // TOML and Config Statement Format Constants
    private static final String CONFIG_STATEMENT_FORMAT = "configurable %s %s = %s;";
    private static final String TOML_KEY_VALUE_FORMAT = "%s = %s";
    private static final String TOML_MODULE_SECTION_FORMAT = "[%s.%s]";
    private static final String TOML_MODULE_WITH_SUBMODULE_SECTION_FORMAT = "[%s.%s.%s]";

    private WorkspaceManager workspaceManager;
    private Gson gson;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    /**
     * Retrieves configuration variables from the Ballerina project.
     *
     * @param request The request containing the project path.
     * @return A {@link CompletableFuture} containing the {@link ConfigVariablesGetResponse}.
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariablesGetResponse> getConfigVariables(ConfigVariableGetRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesGetResponse response = new ConfigVariablesGetResponse();
            // Need to preserve the insertion order (default package first).
            Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
            try {
                Project project = workspaceManager.loadProject(Path.of(request.projectPath()));
                Package rootPackage = project.currentPackage();

                // Parse Config.toml if it exists.
                Toml configTomlValues = parseConfigToml(project);
                handleConfigTomlErrors(configTomlValues, response);

                configVarMap.putAll(extractVariablesFromProject(rootPackage, configTomlValues));
                if (request.includeLibraries()) {
                    configVarMap.putAll(extractConfigsFromDependencies(rootPackage, configTomlValues));
                }

                response.setConfigVariables(gson.toJsonTree(configVarMap));
            } catch (Exception e) {
                response.setError(e);
            }
            return response;
        });
    }

    /**
     * Updates a given configuration variable with the provided value.
     *
     * @param request The request containing the configuration variable and file path.
     * @return A {@link CompletableFuture} containing the {@link ConfigVariableUpdateResponse} with text edits.
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariableUpdateResponse> updateConfigVariable(ConfigVariableUpdateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariableUpdateResponse response = new ConfigVariableUpdateResponse();
            try {
                FlowNode configVariable = gson.fromJson(request.configVariable(), FlowNode.class);
                Path configFilePath = Path.of(request.configFilePath());
                Project rootProject = workspaceManager.loadProject(configFilePath);

                Map<Path, List<TextEdit>> allTextEdits = new HashMap<>();
                // Text edits for Ballerina source files.
                if (requireSourceEdits(configVariable) && isPackageInRootProject(request.packageName(), rootProject)) {
                    allTextEdits.putAll(constructSourceTextEdits(rootProject, configFilePath, configVariable, false));
                }
                // Text edits for Config.toml.
                if (requireConfigTomlEdits(configVariable)) {
                    Toml existingConfigToml = parseConfigToml(rootProject);
                    handleConfigTomlErrors(existingConfigToml, response);
                    Path configTomlPath = rootProject.sourceRoot().resolve(CONFIG_TOML_FILENAME);
                    allTextEdits.putAll(constructConfigTomlTextEdits(rootProject, request.packageName(),
                            request.moduleName(), configVariable, configTomlPath, existingConfigToml, false));
                }

                response.setTextEdits(gson.toJsonTree(allTextEdits));
            } catch (Exception e) {
                response.setError(e);
            }

            return response;
        });
    }

    /**
     * Deletes a given configuration variable.
     *
     * @param request The request containing the configuration variable and file path.
     * @return A {@link CompletableFuture} containing the {@link ConfigVariableDeleteResponse} with text edits.
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariableDeleteResponse> deleteConfigVariable(ConfigVariableDeleteRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariableDeleteResponse response = new ConfigVariableDeleteResponse();
            try {
                FlowNode configVariable = gson.fromJson(request.configVariable(), FlowNode.class);
                Path configFilePath = Path.of(request.configFilePath());
                Project rootProject = workspaceManager.loadProject(configFilePath);

                Map<Path, List<TextEdit>> allTextEdits = new HashMap<>();
                // Text edits for Ballerina source files.
                allTextEdits.putAll(constructSourceTextEdits(rootProject, configFilePath, configVariable, true));

                // Text edits for Config.toml.

                Toml existingConfigToml = parseConfigToml(rootProject);
                handleConfigTomlErrors(existingConfigToml, response);
                Path configTomlPath = rootProject.sourceRoot().resolve(CONFIG_TOML_FILENAME);
                allTextEdits.putAll(constructConfigTomlTextEdits(rootProject, request.packageName(),
                        request.moduleName(), configVariable, configTomlPath, existingConfigToml, true));

                response.setTextEdits(gson.toJsonTree(allTextEdits));
            } catch (Exception e) {
                response.setError(e);
            }

            return response;
        });
    }

    /**
     * Retrieves the node template for configurable variables.
     *
     * @param request The request indicating if the template is for a new variable.
     * @return A {@link CompletableFuture} containing the {@link ConfigVariableNodeTemplateResponse}.
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariableNodeTemplateResponse> getNodeTemplate(
            ConfigVariableNodeTemplateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariableNodeTemplateResponse response = new ConfigVariableNodeTemplateResponse();
            try {
                FlowNode flowNode = getConfigVariableFlowNodeTemplate(request.isNew());
                JsonElement nodeTemplate = gson.toJsonTree(flowNode);
                response.setFlowNode(nodeTemplate);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    /**
     * Constructs text edits for Ballerina source files to update or delete a configuration variable.
     */
    private Map<Path, List<TextEdit>> constructSourceTextEdits(Project rootProject, Path sourceFilePath,
                                                               FlowNode variable, boolean isDelete) {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        try {
            Path variableFilePath = findVariableFilePath(variable, sourceFilePath, rootProject);
            if (variableFilePath == null) {
                return textEditsMap;
            }

            Optional<Document> document = workspaceManager.document(variableFilePath);
            if (document.isEmpty()) {
                return textEditsMap;
            }
            LineRange lineRange = variable.codedata().lineRange();
            String configStatement = constructConfigStatement(variable);

            List<TextEdit> textEdits = new ArrayList<>();
            if (isNew(variable) || lineRange == null) {
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                LinePosition startPos = LinePosition.from(modulePartNode.lineRange().endLine().line() + 1, 0);
                textEdits.add(new TextEdit(CommonUtils.toRange(startPos), configStatement + System.lineSeparator()));
            } else if (isDelete) {
                textEdits.add(new TextEdit(CommonUtils.toRange(lineRange), EMPTY_STRING));
            } else {
                textEdits.add(new TextEdit(CommonUtils.toRange(lineRange), configStatement));
            }

            textEditsMap.put(variableFilePath, textEdits);
            return textEditsMap;
        } catch (RuntimeException e) {
            return textEditsMap;
        }
    }

    /**
     * Constructs the Ballerina source code statement for a configuration variable.
     */
    private static String constructConfigStatement(FlowNode node) {
        String defaultValue = node.properties().get(DEFAULT_VALUE_KEY).toSourceCode();
        String variableDoc = node.properties().get(CONFIG_VAR_DOC_KEY).toSourceCode();
        List<String> docLines = Arrays.stream(variableDoc.split(System.lineSeparator())).toList();

        StringBuilder configStatementBuilder = new StringBuilder();
        docLines.forEach(docLine -> {
                    if (!docLine.isBlank()) {
                        configStatementBuilder
                                .append(HASH_COMMENT_PREFIX)
                                .append(docLine)
                                .append(System.lineSeparator());
                    }
                }
        );

        String variableType = node.properties().get(Property.TYPE_KEY).toSourceCode();
        String variableName = node.properties().get(Property.VARIABLE_KEY).toSourceCode();
        String effectiveDefaultValue = defaultValue.isEmpty() ? QUESTION_MARK : defaultValue;

        configStatementBuilder.append(String.format(CONFIG_STATEMENT_FORMAT,
                variableType,
                variableName,
                effectiveDefaultValue)
        );

        return configStatementBuilder.toString();
    }

    /**
     * Parses the Config.toml file and returns its content as a {@link Toml} object.
     *
     * @param project The Ballerina project.
     * @return A {@link Toml} object representing the parsed Config.toml, or {@code null} if parsing fails
     * or the file doesn't exist.
     */
    private Toml parseConfigToml(Project project) {
        try {
            Path configTomlPath = project.sourceRoot().resolve(CONFIG_TOML_FILENAME);
            if (!Files.exists(configTomlPath)) {
                return null;
            }
            return Toml.read(configTomlPath);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Gets the configuration value for a variable from the parsed Config.toml.
     */
    private Optional<TomlNode> getConfigValue(Toml configValues, String packageName,
                                              String moduleName, String variableName, boolean isRootProject) {
        if (configValues == null) {
            return Optional.empty();
        }

        String pkgNameNormalized = packageName.replace(FORWARD_SLASH, DOT);
        String tomlPkgEntryKey = moduleName.isEmpty() ? pkgNameNormalized :
                String.format("%s.%s", pkgNameNormalized, moduleName);

        // 1. Try to access config values stored with the fully qualified package/module name.
        Optional<Toml> moduleConfigValues = configValues.getTable(tomlPkgEntryKey);
        if (moduleConfigValues.isPresent()) {
            Optional<TomlValueNode> variableValueNode = moduleConfigValues.get().get(variableName);
            if (variableValueNode.isPresent()) {
                return Optional.of(variableValueNode.get());
            }
            Optional<Toml> variableValueTable = moduleConfigValues.get().getTable(variableName);
            if (variableValueTable.isPresent()) {
                return Optional.ofNullable(variableValueTable.get().rootNode());
            }
        }

        // 2. If the module belongs to the root package, try to access directly,
        // as config values can be stored without the package name prefix.
        if (isRootProject) {
            if (moduleName.isEmpty()) {
                Optional<TomlValueNode> variableValueNode = configValues.get(variableName);
                if (variableValueNode.isPresent()) {
                    return Optional.of(variableValueNode.get());
                }
                Optional<Toml> variableValueTable = configValues.getTable(variableName);
                if (variableValueTable.isPresent()) {
                    return Optional.ofNullable(variableValueTable.get().rootNode());
                }
            } else {
                moduleConfigValues = configValues.getTable(moduleName);
                if (moduleConfigValues.isPresent()) {
                    Optional<TomlValueNode> variableValueNode = moduleConfigValues.get().get(variableName);
                    if (variableValueNode.isPresent()) {
                        return Optional.of(variableValueNode.get());
                    }
                    Optional<Toml> variableValueTable = moduleConfigValues.get().getTable(variableName);
                    if (variableValueTable.isPresent()) {
                        return Optional.ofNullable(variableValueTable.get().rootNode());
                    }
                }
            }
        }

        // 3. If the variable is not found, try to access it with a dotted notation.
        String dottedVariableName = String.format("%s.%s", tomlPkgEntryKey, variableName);
        Optional<TomlValueNode> variableValueNode = configValues.get(dottedVariableName);
        if (variableValueNode.isPresent()) {
            return Optional.of(variableValueNode.get());
        }

        return Optional.empty();
    }

    /**
     * Converts a {@link TomlNode} to its string representation.
     */
    private String getAsString(TomlNode tomlValueNode) {
        // In case of syntax errors, return the original string representation of the value in the TOML file.
        boolean hasSyntaxErrors = tomlValueNode.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR);
        if (hasSyntaxErrors) {
            return tomlValueNode.externalTreeNode().toSourceCode();
        }

        switch (tomlValueNode.kind()) {
            case TABLE -> {
                List<String> keyValuePairs = new LinkedList<>();
                ((TomlTableNode) tomlValueNode).entries().forEach((key, topLevelNode) -> {
                    if (topLevelNode.kind() == TomlType.KEY_VALUE) {
                        TomlKeyValueNode keyValueNode = (TomlKeyValueNode) topLevelNode;
                        keyValuePairs.add(key + COLON_SPACE + getAsString(keyValueNode.value()));
                    }
                });
                return OPEN_BRACE + String.join(COMMA_SPACE, keyValuePairs) + CLOSE_BRACE;
            }
            case INTEGER, DOUBLE, BOOLEAN -> {
                return tomlValueNode.toString();
            }
            case STRING -> {
                return DOUBLE_QUOTE + tomlValueNode + DOUBLE_QUOTE;
            }
            case ARRAY -> {
                List<TomlValueNode> elements = ((TomlArrayValueNode) tomlValueNode).elements();
                List<String> elementValues = elements.stream().map(this::getAsString).toList();
                return OPEN_BRACKET + String.join(COMMA_SPACE, elementValues) + CLOSE_BRACKET;
            }
            case TABLE_ARRAY, INLINE_TABLE, UNQUOTED_KEY, KEY_VALUE, NONE -> {
                // TODO: Handle these cases if needed
            }
            default -> {
                return null;
            }
        }
        return null;
    }

    /**
     * Extracts configuration variables from the current package and its submodules.
     */
    private Map<String, Map<String, List<FlowNode>>> extractVariablesFromProject(
            Package rootPackage, Toml configTomlValues) {
        Map<String, List<FlowNode>> moduleConfigVarMap = new HashMap<>();
        String pkgName = rootPackage.packageOrg().value() + FORWARD_SLASH + rootPackage.packageName().value();

        for (Module module : rootPackage.modules()) {
            String modName = module.moduleName().moduleNamePart() != null ?
                    module.moduleName().moduleNamePart() : EMPTY_STRING;
            List<FlowNode> variables = extractModuleConfigVariables(module, configTomlValues, pkgName, modName, true);
            moduleConfigVarMap.put(modName, variables);
        }

        Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
        configVarMap.put(pkgName, moduleConfigVarMap);
        return configVarMap;
    }

    private Path findVariableFilePath(FlowNode configVariable, Path contextFilePath, Project rootProject) {
        if (isNew(configVariable)) {
            return contextFilePath;
        }

        if (configVariable.codedata() == null || configVariable.codedata().lineRange() == null) {
            return null;
        }
        String variableFileName = configVariable.codedata().lineRange().fileName();

        if (rootProject.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return rootProject.sourceRoot();
        }

        for (Module module : rootProject.currentPackage().modules()) {
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                if (document.name().equals(variableFileName)) {
                    return rootProject.sourceRoot().resolve(document.syntaxTree().filePath());
                }
            }
        }
        return null;
    }

    /**
     * Checks if a configuration variable is new (i.e., does not yet exist in source code).
     */
    private boolean isNew(FlowNode configVariable) {
        return configVariable.codedata() != null
                && configVariable.codedata().isNew() != null
                && configVariable.codedata().isNew();
    }

    /**
     * Extracts configuration variables from the given module.
     */
    private List<FlowNode> extractModuleConfigVariables(Module module, Toml configTomlValues,
                                                        String packageName, String moduleName, boolean isRootProject) {
        List<FlowNode> configVariables = new LinkedList<>();
        Optional<SemanticModel> semanticModel = getSemanticModel(module);

        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            SyntaxTree syntaxTree = document.syntaxTree();
            if (syntaxTree == null) {
                continue;
            }
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            for (Node node : modulePartNode.members()) {
                if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
                    ModuleVariableDeclarationNode varDeclarationNode = (ModuleVariableDeclarationNode) node;
                    if (hasConfigurableQualifier(varDeclarationNode)) {
                        FlowNode configVarNode = constructConfigVarNode(varDeclarationNode, semanticModel.orElse(null),
                                configTomlValues, packageName, moduleName, isRootProject);
                        configVariables.add(configVarNode);
                    }
                }
            }
        }
        return configVariables;
    }

    /**
     * Retrieves the {@link SemanticModel} for a given module.
     *
     * @param module The module.
     * @return An {@link Optional} containing the {@link SemanticModel}, or empty if unavailable.
     */
    private static Optional<SemanticModel> getSemanticModel(Module module) {
        try {
            if (module.packageInstance() != null && module.packageInstance().getCompilation() != null) {
                SemanticModel semanticModel = module.packageInstance().getCompilation()
                        .getSemanticModel(module.moduleId());
                return Optional.ofNullable(semanticModel);
            }
        } catch (RuntimeException e) {
            // getSemanticModel() can throw an Error if the module is an imported module without a semantic model.
        }
        return Optional.empty();
    }

    /**
     * Extracts configuration variables from the dependencies of the current package.
     */
    private Map<String, Map<String, List<FlowNode>>> extractConfigsFromDependencies(
            Package currentPackage, Toml configTomlValues) {
        Map<String, Map<String, List<FlowNode>>> dependencyConfigVarMap = new HashMap<>();
        for (Module module : currentPackage.modules()) {
            List<ModuleDependency> validDependencies = new LinkedList<>();
            populateValidDependencies(currentPackage, module, validDependencies);
            dependencyConfigVarMap.putAll(getImportedConfigVars(currentPackage, validDependencies, configTomlValues));
        }
        return dependencyConfigVarMap;
    }

    /**
     * Checks if a {@link ModuleVariableDeclarationNode} has the 'configurable' qualifier.
     */
    private static boolean hasConfigurableQualifier(ModuleVariableDeclarationNode node) {
        return node.qualifiers()
                .stream()
                .anyMatch(q -> q.text().equals(Qualifier.CONFIGURABLE.getValue()));
    }

    /**
     * Retrieves configurable variables for all direct imports of a package.
     */
    private Map<String, Map<String, List<FlowNode>>> getImportedConfigVars(
            Package currentPkg, Collection<ModuleDependency> moduleDependencies,
            Toml configTomlValues) {
        Map<String, Map<String, List<FlowNode>>> pkgConfigs = new HashMap<>();
        if (currentPkg.getResolution() == null || currentPkg.getResolution().dependencyGraph() == null) {
            return pkgConfigs;
        }
        Collection<ResolvedPackageDependency> dependencies = currentPkg.getResolution().dependencyGraph().getNodes();
        for (ResolvedPackageDependency dependency : dependencies) {
            if (dependency.packageInstance() == null || !isDirectDependency(dependency, moduleDependencies)) {
                continue;
            }

            Map<String, List<FlowNode>> moduleConfigs = processDependency(dependency, configTomlValues);
            if (!moduleConfigs.isEmpty()) {
                String pkgKey = dependency.packageInstance().packageOrg().value() + FORWARD_SLASH +
                        dependency.packageInstance().packageName().value();
                pkgConfigs.put(pkgKey, moduleConfigs);
            }
        }
        return pkgConfigs;
    }

    /**
     * Processes a resolved package dependency to extract its configuration variables.
     */
    private Map<String, List<FlowNode>> processDependency(ResolvedPackageDependency dependency,
                                                          Toml configTomlValues) {
        Map<String, List<FlowNode>> moduleConfigs = new HashMap<>();
        String packageName = dependency.packageInstance().packageOrg().value() + FORWARD_SLASH +
                dependency.packageInstance().packageName().value();

        for (Module module : dependency.packageInstance().modules()) {
            String moduleName = module.moduleName().moduleNamePart() != null ?
                    module.moduleName().moduleNamePart() : EMPTY_STRING;
            List<FlowNode> variables = extractModuleConfigVariables(module, configTomlValues, packageName, moduleName,
                    false);
            if (!variables.isEmpty()) {
                moduleConfigs.put(moduleName, variables);
            }
        }
        return moduleConfigs;
    }

    /**
     * Populates a collection with valid module dependencies for a given package and module.
     * Valid dependencies are those with default scope and not belonging to the same package.
     */
    private static void populateValidDependencies(Package packageInstance, Module module,
                                                  Collection<ModuleDependency> dependencies) {
        for (ModuleDependency moduleDependency : module.moduleDependencies()) {
            if (!isDefaultScope(moduleDependency) || isSamePackage(packageInstance, moduleDependency)) {
                continue;
            }
            dependencies.add(moduleDependency);
        }
    }

    /**
     * Checks if the dependency has the default scope.
     *
     * @param moduleDependency The module dependency.
     * @return {@code true} if the scope is default, {@code false} otherwise.
     */
    private static boolean isDefaultScope(ModuleDependency moduleDependency) {
        return moduleDependency.packageDependency().scope().getValue().equals(
                PlatformLibraryScope.DEFAULT.getStringValue());
    }

    /**
     * Checks if the dependency is from the same package as the provided package instance.
     */
    private static boolean isSamePackage(Package packageInstance, ModuleDependency moduleDependency) {
        String orgValue = moduleDependency.descriptor().org().value();
        String packageVal = moduleDependency.descriptor().packageName().value();
        return orgValue.equals(packageInstance.packageOrg().value())
                && packageVal.equals(packageInstance.packageName().value());
    }

    /**
     * Checks if a given resolved package dependency is a direct dependency based on a collection of module
     * dependencies.
     */
    private static boolean isDirectDependency(ResolvedPackageDependency dep,
                                              Collection<ModuleDependency> moduleDependencies) {
        PackageDescriptor descriptor = dep.packageInstance().descriptor();
        String orgName = descriptor.org().value();
        String packageName = descriptor.name().value();

        for (ModuleDependency dependency : moduleDependencies) {
            if (dependency.descriptor().org().value().equals(orgName) &&
                    dependency.descriptor().packageName().value().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a {@link FlowNode} for a configuration variable, incorporating its value from Config.toml
     * if available.
     */
    private FlowNode constructConfigVarNode(ModuleVariableDeclarationNode variableNode,
                                            SemanticModel semanticModel, Toml configTomlValues,
                                            String packageName, String moduleName, boolean isRootProject) {

        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.CONFIG_VARIABLE)
                .semanticModel(semanticModel)
                .defaultModuleName(null);

        if (semanticModel != null) {
            DiagnosticHandler diagnosticHandler = new DiagnosticHandler(semanticModel);
            diagnosticHandler.handle(nodeBuilder, variableNode.lineRange(), false);
            nodeBuilder.diagnosticHandler(diagnosticHandler);
        }

        TypedBindingPatternNode typedBindingPattern = variableNode.typedBindingPattern();
        String variableName = typedBindingPattern.bindingPattern().toSourceCode().trim();
        Optional<Node> markdownDocs = extractVariableDocs(variableNode);

        // Get the configuration value from Config.toml.
        Optional<TomlNode> configTomlValue = getConfigValue(configTomlValues, packageName, moduleName, variableName,
                isRootProject);
        ExpressionNode configValueExpr = null;
        if (configTomlValue.isPresent()) {
            String tomlStringValue = getAsString(configTomlValue.get());
            if (tomlStringValue != null) {
                configValueExpr = NodeParser.parseExpression(tomlStringValue);
            }
        }

        return nodeBuilder
                .metadata()
                .stepOut()
                .codedata()
                .node(NodeKind.CONFIG_VARIABLE)
                .lineRange(variableNode.lineRange())
                .stepOut()
                .properties()
                .variableName(variableName, isRootProject)
                .type(typedBindingPattern.typeDescriptor(), isRootProject)
                .defaultValue(variableNode.initializer().orElse(null), isRootProject)
                .configValue(configValueExpr)
                .documentation(markdownDocs.orElse(null), isRootProject)
                .stepOut()
                .build();
    }

    /**
     * Extracts the markdown documentation from the variable node.
     */
    private static Optional<Node> extractVariableDocs(ModuleVariableDeclarationNode variableNode) {
        Optional<MetadataNode> metadata = variableNode.metadata();
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        return metadata.get().documentationString();
    }

    /**
     * Creates a template {@link FlowNode} for a configuration variable.
     */
    private static FlowNode getConfigVariableFlowNodeTemplate(boolean isNew) {
        NodeBuilder nodeBuilder = NodeBuilder
                .getNodeFromKind(NodeKind.CONFIG_VARIABLE)
                .defaultModuleName(null);

        nodeBuilder = nodeBuilder
                .metadata()
                .stepOut()
                .codedata()
                .node(NodeKind.CONFIG_VARIABLE)
                .lineRange(null)
                .isNew(isNew)
                .stepOut();

        if (isNew) {
            nodeBuilder = nodeBuilder
                    .properties()
                    .variableName(null, true, false)
                    .type(null, true, false)
                    .configValue(null)
                    .defaultValue(null)
                    .documentation(null)
                    .stepOut();
        } else {
            nodeBuilder = nodeBuilder
                    .properties()
                    .variableName(null, true, false)
                    .type(null, true, false)
                    .defaultValue(null)
                    .documentation(null)
                    .stepOut();
        }

        return nodeBuilder.build();
    }

    /**
     * Constructs text edits for updating the Config.toml file with a new configuration value or deleting an entry.
     */
    private Map<Path, List<TextEdit>> constructConfigTomlTextEdits(Project project, String packageName,
                                                                   String moduleName, FlowNode configVariable,
                                                                   Path configTomlPath, Toml existingConfigToml,
                                                                   boolean isDelete) {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        try {
            if (!configVariable.properties().containsKey(CONFIG_VALUE_KEY)) {
                return textEditsMap;
            }

            Property nameProp = configVariable.properties().get(VARIABLE_KEY);
            String oldVariableName = isNameModified(nameProp) ? nameProp.oldValue().toString()
                    : nameProp.value().toString();

            Optional<TomlNode> oldConfigValue = getConfigValue(existingConfigToml, packageName, moduleName,
                    oldVariableName, isPackageInRootProject(packageName, project));
            String configValue = configVariable.properties().get(CONFIG_VALUE_KEY).toSourceCode();
            if (isDelete && oldConfigValue.isEmpty()) {
                return textEditsMap;
            } else if (oldConfigValue.isEmpty() && configValue.isEmpty()) {
                return textEditsMap;
            }

            String[] pkgParts = packageName.split(FORWARD_SLASH);
            String orgName = pkgParts.length > 0 ? pkgParts[0] : EMPTY_STRING;
            String pkgName = pkgParts.length > 1 ? pkgParts[1] : EMPTY_STRING;
            String variableName = nameProp.value().toString();

            String newContent = isDelete || configValue.isEmpty() ? EMPTY_STRING : constructConfigTomlStatement(
                    orgName, pkgName, moduleName, variableName, configValue, oldConfigValue.isPresent());

            List<TextEdit> textEdits = new ArrayList<>();
            if (oldConfigValue.isPresent()) {
                String fileName = oldConfigValue.get().location().lineRange().fileName();
                LineRange lineRange = oldConfigValue.get().location().lineRange();
                LinePosition startPos = LinePosition.from(lineRange.startLine().line(), 0);
                LinePosition endPos = LinePosition.from(lineRange.endLine().line(), lineRange.endLine().offset());

                LineRange newlineRange = LineRange.from(fileName, startPos, endPos);
                textEdits.add(new TextEdit(CommonUtils.toRange(newlineRange), newContent));
            } else {
                // if the variable is new, we need to find the relevant section in Config.toml file and add the new
                // entry after the last entry of the section.
                if (existingConfigToml != null) {
                    // Try to find existing section for the module
                    String sectionKey = moduleName.isEmpty() ? String.format("%s.%s", orgName, pkgName)
                            : String.format("%s.%s.%s", orgName, pkgName, moduleName);

                    Optional<Toml> moduleSection = existingConfigToml.getTable(sectionKey);
                    if (moduleSection.isPresent()) {
                        // Section exists - find the last entry and add after it
                        TomlTableNode moduleTableNode = moduleSection.get().rootNode();
                        if (!moduleTableNode.entries().isEmpty()) {
                            // Get the last entry in the section
                            TomlNode lastEntry = moduleTableNode.entries().values().stream()
                                    .reduce((first, second) -> second)
                                    .orElse(null);

                            if (lastEntry != null) {
                                LinePosition insertPos = LinePosition.from(
                                        lastEntry.location().lineRange().endLine().line() + 1, 0);
                                textEdits.add(new TextEdit(CommonUtils.toRange(insertPos),
                                        String.format("%s = %s%n", variableName, configValue)));
                            }
                        } else {
                            // Section exists but is empty - add right after section header
                            LinePosition insertPos = LinePosition.from(
                                    moduleTableNode.location().lineRange().endLine().line() + 1, 0);
                            textEdits.add(new TextEdit(CommonUtils.toRange(insertPos),
                                    String.format("%s = %s%n", variableName, configValue)));
                        }
                    } else {
                        // Section doesn't exist - append to end of file
                        TomlTableNode rootNode = existingConfigToml.rootNode();
                        LinePosition insertPos;

                        if (!rootNode.entries().isEmpty()) {
                            // Find the last entry in the root table
                            TomlNode lastEntry = rootNode.entries().values().stream()
                                    .reduce((first, second) -> second)
                                    .orElse(null);
                            insertPos = LinePosition.from(lastEntry.location().lineRange().endLine().line() + 1, 0);
                        } else {
                            // Empty config file
                            insertPos = LinePosition.from(0, 0);
                        }

                        textEdits.add(new TextEdit(CommonUtils.toRange(insertPos),
                                String.format("%n%s%n", newContent)));
                    }
                } else {
                    // Config.toml doesn't exist - create new file with the content
                    if (!Files.exists(configTomlPath)) {
                        try {
                            Files.createFile(configTomlPath);
                        } catch (Exception createEx) {
                            // Handle file creation error
                            return textEditsMap;
                        }
                    }
                    LinePosition startPos = LinePosition.from(0, 0);
                    textEdits.add(new TextEdit(CommonUtils.toRange(startPos), newContent + System.lineSeparator()));
                }
            }

            textEditsMap.put(configTomlPath, textEdits);
            return textEditsMap;
        } catch (RuntimeException e) {
            return new HashMap<>();
        }
    }

    /**
     * Checks if the configurable variable name has been modified.
     */
    private static boolean isNameModified(Property nameProp) {
        return Boolean.TRUE.equals(nameProp.modified()) && nameProp.oldValue() != null;
    }

    /**
     * Checks if the given package name corresponds to the root project's package.
     */
    private static boolean isPackageInRootProject(String packageName, Project rootProject) {
        if (rootProject == null || rootProject.currentPackage() == null) {
            return false;
        }
        String rootPackageName = rootProject.currentPackage().packageOrg().value() + FORWARD_SLASH +
                rootProject.currentPackage().packageName().value();
        return packageName.equals(rootPackageName);
    }

    /**
     * Constructs a TOML statement for a configuration variable.
     */
    private static String constructConfigTomlStatement(String orgName, String packageName, String moduleName,
                                                       String variableName, String value, boolean sectionHeaderExists) {
        ExpressionNode configValueExpr = NodeParser.parseExpression(value);
        String tomlValue = getInTomlSyntax(configValueExpr);

        if (sectionHeaderExists) {
            return String.format(TOML_KEY_VALUE_FORMAT, variableName, tomlValue);
        } else {
            String sectionHeader;
            if (moduleName.isEmpty()) {
                sectionHeader = String.format(TOML_MODULE_SECTION_FORMAT, orgName, packageName);
            } else {
                sectionHeader = String.format(TOML_MODULE_WITH_SUBMODULE_SECTION_FORMAT,
                        orgName, packageName, moduleName);
            }
            return String.format("%s%n%s", sectionHeader, String.format(TOML_KEY_VALUE_FORMAT,
                    variableName, tomlValue));
        }
    }

    /**
     * Converts a Ballerina {@link ExpressionNode} into its TOML syntax string representation.
     */
    private static String getInTomlSyntax(ExpressionNode configValueExpr) {
        switch (configValueExpr.kind()) {
            case MAPPING_CONSTRUCTOR -> {
                MappingConstructorExpressionNode recordTypeDesc = (MappingConstructorExpressionNode) configValueExpr;
                StringBuilder sb = new StringBuilder(OPEN_BRACE);
                for (MappingFieldNode field : recordTypeDesc.fields()) {
                    if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                        SpecificFieldNode mappingField = (SpecificFieldNode) field;
                        String key = mappingField.fieldName().toSourceCode();
                        String value = mappingField.valueExpr().isPresent() ?
                                getInTomlSyntax(mappingField.valueExpr().get()) : null;
                        if (value != null) {
                            sb.append(key).append(EQUALS_SIGN_SPACED).append(value).append(COMMA_SPACE);
                        }
                    }
                }
                if (sb.length() > 1) {
                    sb.setLength(sb.length() - 2);
                }
                sb.append(CLOSE_BRACE);
                return sb.toString();
            }
            case LIST_CONSTRUCTOR -> {
                ListConstructorExpressionNode arrayTypeDesc = (ListConstructorExpressionNode) configValueExpr;
                StringBuilder sbArray = new StringBuilder(OPEN_BRACKET);
                List<String> memberValues = new LinkedList<>();
                for (Node element : arrayTypeDesc.expressions()) {
                    if (element instanceof ExpressionNode expressionNode) {
                        memberValues.add(getInTomlSyntax(expressionNode));
                    }
                }
                sbArray.append(String.join(COMMA_SPACE, memberValues));
                sbArray.append(CLOSE_BRACKET);
                return sbArray.toString();
            }
            default -> {
                // TODO: Add support for other types if needed
                return configValueExpr.toSourceCode();
            }
        }
    }

    /**
     * Checks if the Ballerina source edits are required when updating a configuration variable, based on its modified
     * properties.
     */
    private boolean requireSourceEdits(FlowNode configVariable) {
        Property variableName = configVariable.properties().get(VARIABLE_KEY);
        Property variableType = configVariable.properties().get(Property.TYPE_KEY);
        Property defaultValue = configVariable.properties().get(DEFAULT_VALUE_KEY);
        Property documentation = configVariable.properties().get(CONFIG_VAR_DOC_KEY);

        return (variableName != null && Boolean.TRUE.equals(variableName.modified()))
                || (variableType != null && Boolean.TRUE.equals(variableType.modified()))
                || (defaultValue != null && Boolean.TRUE.equals(defaultValue.modified()))
                || (documentation != null && Boolean.TRUE.equals(documentation.modified()));
    }

    /**
     * Checks if the Config.toml edits are required when updating a configuration variable, based on its modified
     * properties.
     */
    private boolean requireConfigTomlEdits(FlowNode configVariable) {
        Property variableName = configVariable.properties().get(VARIABLE_KEY);
        Property configValue = configVariable.properties().get(CONFIG_VALUE_KEY);
        return (variableName != null && Boolean.TRUE.equals(variableName.modified()))
                || (configValue != null && Boolean.TRUE.equals(configValue.modified()));
    }

    private static void handleConfigTomlErrors(Toml configTomlValues, AbstractFlowModelResponse response) {
        boolean hasErrors = configTomlValues != null && configTomlValues.diagnostics().stream()
                .anyMatch(d -> d.diagnosticInfo().severity() == DiagnosticSeverity.ERROR);
        if (hasErrors) {
            IllegalStateException exception = new IllegalStateException("Config.toml contains syntax errors. " +
                    "Features like config editing, local run, and debugging may not function properly " +
                    "until the file is manually fixed.");
            response.setError(exception);
        }
    }
}
