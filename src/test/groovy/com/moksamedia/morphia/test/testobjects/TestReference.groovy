package com.moksamedia.morphia.test.testobjects

import org.bson.types.ObjectId;

import com.github.jmkgreen.morphia.annotations.Id;

class TestReference {

	@Id
	private ObjectId id;

	String value
}
