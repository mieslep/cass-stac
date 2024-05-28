package com.datastax.oss.cass_stac.dao;

public interface IDao<T> {
    void initialize();
    void save(T object);
    T get(Object id);
}
