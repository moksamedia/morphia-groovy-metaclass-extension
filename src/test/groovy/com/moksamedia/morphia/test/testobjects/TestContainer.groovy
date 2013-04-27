package com.moksamedia.morphia.test.testobjects

import org.bson.types.ObjectId;

import com.github.jmkgreen.morphia.annotations.Entity
import com.github.jmkgreen.morphia.annotations.Id
import com.mongodb.DBObject

@Entity
class TestContainer {

	@Id
	private ObjectId id;
	
	//String value = "CONTAINER"
	
}
