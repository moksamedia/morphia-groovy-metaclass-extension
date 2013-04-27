package com.moksamedia.morphia.test.testobjects

import com.moksamedia.morphia.ClassToExpand

@ClassToExpand(TestContainer)
class TestContainerExpanderPrimitive {

	int num = 5
	String string = "FIVE"
	Date date = new Date(1111111)
	
}
