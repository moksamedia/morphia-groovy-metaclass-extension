package com.moksamedia.morphia.test.testobjects

import com.moksamedia.morphia.ClassToExpand

@ClassToExpand(TestContainer)
class TestContainerExpanderCollection {

	List<TestEmbedded> embedded = []
	List<TestReference> reference = []
	
}
