package org.odk.collect.onic.injection;

public interface DependencyProvider<T> {
    T provide();
}
