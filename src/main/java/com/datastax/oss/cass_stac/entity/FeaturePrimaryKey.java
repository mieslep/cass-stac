package com.datastax.oss.cass_stac.entity;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@Data
@PrimaryKeyClass
public class FeaturePrimaryKey implements Serializable{

	private static final long serialVersionUID = 6214163699083729797L;
	
	@PrimaryKeyColumn(type=PrimaryKeyType.PARTITIONED)
	private String partition_id;
	
	@PrimaryKeyColumn(type=PrimaryKeyType.CLUSTERED, ordinal = 0)
	private String id;

}
