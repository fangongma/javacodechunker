package jp.co.jri.codechunker.model.chunk;

public enum ChunkLevel {
    CLASS_LEVEL("CLASS"),
    METHOD_LEVEL("METHOD");

    private final String value;

    ChunkLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ChunkLevel fromString(String value) {
        return switch (value.toUpperCase()) {
            case "CLASS" -> CLASS_LEVEL;
            case "METHOD" -> METHOD_LEVEL;
            default -> throw new IllegalArgumentException("Unknown chunk level: " + value);
        };
    }
}