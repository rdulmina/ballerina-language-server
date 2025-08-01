package io.ballerina.flowmodelgenerator.core.model;

/**
 * This is a class used to identify the type of the JSON kind. The kind is used for identifying both nodes and
 * branches.
 *
 * @since 1.0.0
 */
public enum NodeKind {
    // Flow nodes
    EVENT_START,
    MCP_TOOLKIT,
    IF,
    REMOTE_ACTION_CALL,
    RESOURCE_ACTION_CALL,
    FUNCTION_CALL,
    METHOD_CALL,
    NEW_CONNECTION,
    RETURN,
    EXPRESSION,
    ERROR_HANDLER,
    WHILE,
    CONTINUE,
    BREAK,
    PANIC,
    START,
    TRANSACTION,
    RETRY,
    LOCK,
    FAIL,
    COMMIT,
    ROLLBACK,
    VARIABLE,
    CONFIG_VARIABLE,
    ASSIGN,
    XML_PAYLOAD,
    JSON_PAYLOAD,
    BINARY_DATA,
    STOP,
    FOREACH,
    DATA_MAPPER,
    DATA_MAPPER_DEFINITION,
    FUNCTION_DEFINITION,
    AUTOMATION,
    NP_FUNCTION_CALL,
    NP_FUNCTION,
    NP_FUNCTION_DEFINITION,
    COMMENT,
    MATCH,
    FUNCTION,
    FORK,
    PARALLEL_FLOW,
    WAIT,
    DATA_MAPPER_CALL,

    // Branches
    CONDITIONAL,
    ELSE,
    ON_FAILURE,
    BODY,
    WORKER,

    // Types
    RECORD,
    ENUM,
    ARRAY,
    UNION,
    ERROR,
    MAP,
    STREAM,
    FUTURE,
    TYPEDESC,
    CLASS,
    OBJECT,
    INTERSECTION,
    SERVICE_DECLARATION,
    TABLE,
    TUPLE,

    AGENT,
    AGENT_CALL,
    CLASS_INIT,

    MODEL_PROVIDER,
    MODEL_PROVIDERS,
    EMBEDDING_PROVIDER,
    EMBEDDING_PROVIDERS,
    VECTOR_STORE,
    VECTOR_STORES,
    VECTOR_KNOWLEDGE_BASE,
    VECTOR_KNOWLEDGE_BASES
}
