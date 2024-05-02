enum ObjectType {
    Blob, Tree;

    public static ObjectType parse(String type) {
        return switch (type) {
            case "blob" -> Blob;
            case "tree" -> Tree;
            default -> throw new IllegalArgumentException("invalid object type: %s".formatted(type));
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case Blob -> "blob";
            case Tree -> "tree";
        };
    }
}
