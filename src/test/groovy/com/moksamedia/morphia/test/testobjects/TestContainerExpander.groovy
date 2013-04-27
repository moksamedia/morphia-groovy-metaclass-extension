package com.moksamedia.morphia.test.testobjects

import com.github.jmkgreen.morphia.annotations.Embedded
import com.github.jmkgreen.morphia.annotations.Reference
import com.moksamedia.morphia.ClassToExpand


@ClassToExpand(TestContainer)
class TestContainerExpander {

	@Reference
	TestReference reference = null
	
	@Embedded
	TestEmbedded embedded = null
	
}
