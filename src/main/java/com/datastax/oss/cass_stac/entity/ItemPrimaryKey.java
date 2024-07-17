package com.datastax.oss.cass_stac.entity;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.Data;

@Data
@PrimaryKeyClass
public class ItemPrimaryKey implements Serializable{

	private static final long serialVersionUID = 5214163699083729797L;
	
	@PrimaryKeyColumn(type=PrimaryKeyType.PARTITIONED, ordinal = 0)
	private String partition_id;
	
	@PrimaryKeyColumn(type=PrimaryKeyType.CLUSTERED, ordinal = 1)
	private String id;

}
