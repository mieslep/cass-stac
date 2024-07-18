package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.entity.Feature;
import com.datastax.oss.cass_stac.entity.FeaturePrimaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureDao extends  CassandraRepository<Feature, FeaturePrimaryKey>{

	

}
