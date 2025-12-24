package jp.co.jri.codechunker.model.chunk;

public enum Kind {
    UNKNOWN,
    FILE,

    // Type Kind
    INTERFACE,
    CLASS,
    STRUCT,
    ENUM,
    DELEGATE,
    RECORD,

    // Member Kind
    CONSTRUCTOR,
    DESTRUCTOR,
    METHOD,
    PROPERTY,
    VARIABLE,
    EVENT,
    INDEXER,
    OPERATOR,
    NESTEDTYPE
}
