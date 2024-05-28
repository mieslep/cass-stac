package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.driver.api.core.CqlSession;

public abstract class ADao<T> implements IDao<T> {
    protected final CqlSession session;
    protected final GeoTimePartition partitioner;

    protected ADao(CqlSession session, GeoTimePartition partitioner) {
        this.session = session;
        this.partitioner = partitioner;
        initialize();
    }

    @Override
    public abstract void initialize();

    @Override
    public abstract void save(T object);

    @Override
    public abstract T get(Object id);
}
