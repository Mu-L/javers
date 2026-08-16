package org.javers.core.metamodel.property;

/**
 * Internal marker for a property present on only one side of a comparison.
 * Must not leak into user comparators (see #1076).
 */
public class MissingProperty {
    public static final MissingProperty INSTANCE = new MissingProperty();

    private MissingProperty() {
    }

    /**
     * Converts {@link #INSTANCE} to {@code null}; other values pass through.
     * Aligns with {@link org.javers.core.diff.changetype.Atomic#unwrap()}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Object value) {
        if (value == INSTANCE) {
            return null;
        }
        return (T) value;
    }

    @Override
    public String toString() {
        return "MISSING_PROPERTY";
    }
}
