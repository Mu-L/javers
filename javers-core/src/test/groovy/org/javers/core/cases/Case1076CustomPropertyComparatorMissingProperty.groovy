package org.javers.core.cases

import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.PropertyChangeMetadata
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.custom.CustomPropertyComparator
import org.javers.core.diff.custom.CustomValueComparator
import org.javers.core.metamodel.clazz.EntityDefinition
import org.javers.core.metamodel.property.MissingProperty
import org.javers.core.metamodel.property.Property
import org.javers.core.metamodel.type.CustomComparableType
import spock.lang.Specification

/**
 * https://github.com/javers/javers/issues/1076
 *
 * MissingProperty must not be cast/passed into CustomPropertyComparator as T.
 */
class Case1076CustomPropertyComparatorMissingProperty extends Specification {

    static class Container {
        int id
        Parent parent

        Container(int id, Parent parent) {
            this.id = id
            this.parent = parent
        }
    }

    static class Parent {}

    static class ChildWithText extends Parent {
        String text

        ChildWithText(String text) {
            this.text = text
        }
    }

    static class ChildWithoutText extends Parent {}

    /**
     * Intentionally assumes both args are String or null (never MissingProperty).
     * A ClassCastException from MissingProperty proves the bug.
     */
    static class NullSafeStringComparator implements CustomPropertyComparator<String, ValueChange> {
        @Override
        Optional<ValueChange> compare(String left, String right, PropertyChangeMetadata metadata, Property property) {
            if (equals(left, right)) {
                return Optional.empty()
            }
            return Optional.of(new ValueChange(metadata, left, right))
        }

        @Override
        boolean equals(String a, String b) {
            Objects.equals(normalize(a), normalize(b))
        }

        @Override
        String toString(String value) {
            return normalize(value)
        }

        private static String normalize(String s) {
            return s == null ? "" : s
        }
    }

    /**
     * Fails hard if MissingProperty leaks into toString (ClassCastException / wrong type).
     */
    static class StrictStringValueComparator implements CustomValueComparator<String> {
        @Override
        boolean equals(String a, String b) {
            Objects.equals(a, b)
        }

        @Override
        String toString(String value) {
            return value.toUpperCase()
        }
    }

    def "should not pass MissingProperty into CustomPropertyComparator when a property exists on only one side"() {
        given:
        def javers = JaversBuilder.javers()
                .registerEntity(new EntityDefinition(Container.class, "id"))
                .registerCustomType(String, new NullSafeStringComparator())
                .build()

        def left = new Container(1, new ChildWithText("hello"))
        def right = new Container(1, new ChildWithoutText())

        when:
        def diff = javers.compare(left, right)

        then:
        noExceptionThrown()
        diff.changes.size() >= 1
    }

    def "should treat MissingProperty as null for CustomPropertyComparator (null vs value is a change)"() {
        given:
        def javers = JaversBuilder.javers()
                .registerEntity(new EntityDefinition(Container.class, "id"))
                .registerCustomType(String, new NullSafeStringComparator())
                .build()

        def left = new Container(1, new ChildWithText("hello"))
        def right = new Container(1, new ChildWithoutText())

        when:
        def diff = javers.compare(left, right)

        then:
        def textChanges = diff.changes.findAll {
            it instanceof ValueChange && it.propertyName == "text"
        }
        textChanges.size() == 1
        def change = textChanges[0] as ValueChange
        change.left == "hello"
        change.right == null
    }

    def "should not pass MissingProperty into CustomValueComparator.toString()"() {
        given:
        def javers = JaversBuilder.javers()
                .registerValue(String, new StrictStringValueComparator())
                .build()
        def stringType = javers.getTypeMapping(String) as CustomComparableType

        expect:
        stringType.valueToString(MissingProperty.INSTANCE) == ""
        stringType.valueToString(null) == ""
        stringType.valueToString("ab") == "AB"
    }
}
