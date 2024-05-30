package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;

public interface IDao<T> {
    void initialize() throws ConfigException;
    void save(T object) throws DaoException;
    T get(String partitionId, Object id) throws DaoException;
}
