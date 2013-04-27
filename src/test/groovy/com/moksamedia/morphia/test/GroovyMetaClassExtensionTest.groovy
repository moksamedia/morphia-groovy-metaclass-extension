package com.moksamedia.morphia.test

import groovy.util.logging.Slf4j

import java.lang.reflect.Field

import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import com.github.jmkgreen.morphia.Datastore
import com.github.jmkgreen.morphia.Morphia
import com.moksamedia.morphia.GroovyMetaClassExtension
import com.moksamedia.morphia.test.testobjects.TestContainer
import com.moksamedia.morphia.test.testobjects.TestContainerExpander
import com.moksamedia.morphia.test.testobjects.TestContainerExpanderCollection
import com.moksamedia.morphia.test.testobjects.TestContainerExpanderPrimitive
import com.moksamedia.morphia.test.testobjects.TestEmbedded
import com.moksamedia.morphia.test.testobjects.TestReference
import com.mongodb.DB
import com.mongodb.MongoClient

/*
 * To run the tests, a mongo database must be running on the hostname and port
 * specified in the "src/test/resources/config.groovy" file, which defaults to
 * localhost and 27017. The default test database name that will be created is 
 * "morpha-groovy-metaclass-extension-test".
 */
@Slf4j
class GroovyMetaClassExtensionTest {

	public static String testDBName
	public static int testDBPort
	public static String testDBHost

	protected Morphia morphia
	protected DB db 
	protected Datastore ds
	protected MongoClient mongo
	
	/*
	 * Load the database name, port, and hostname from the config file.
	 */
	@BeforeClass
	public static void loadConfig() {
				
		URL urlToConfig  = GroovyMetaClassExtensionTest.classLoader.getResource('config.groovy')
		ConfigObject config = new ConfigSlurper().parse(urlToConfig)

		testDBName = config.testDBName
		testDBPort = config.testDBPort
		testDBHost = config.testDBHost

	}
	
	@Before
	void loadMongo() {

		log.info "Creating MongoClient, Morhpia instance, and mapping classes"
		
		mongo = new MongoClient(testDBHost, testDBPort)

		morphia = new Morphia()

		morphia.map(TestContainer)
		morphia.map(TestReference)
		morphia.map(TestEmbedded)

		ds = morphia.createDatastore(mongo, testDBName)
		db = mongo.getDB(testDBName);
	}
	
	@After
	void doAfter() {
		log.info "Cleaning up: closing mongo and dropping database"
		dropDB()
		mongo.close()
	}
	
	protected void dropDB() {
		db.dropDatabase()
	}

	/*
	 * Verifies that the GroovyMetaClassExtension.load() method is
	 * working properly.
	 */
	@Test
	void testLoadExpanderClass() {
		
		log.info "Testing load expander class functionality"
		// Test load TestContainerExpanderPrimitive
		
		GroovyMetaClassExtension ext = new GroovyMetaClassExtension(morphia)
		ext.load(TestContainerExpanderPrimitive)

		assert ext.expandedClasses.containsKey(TestContainer)
		
		List fields = ext.expandedClasses.get(TestContainer)
		
		assert fields.size() == 3
		
		Field field = fields.find {it.name == 'num'}
		
		assert field != null
		assert field.type == int
		
		field = fields.find {it.name == 'string'}
		
		assert field != null
		assert field.type == String

		field = fields.find {it.name == 'date'}
		
		assert field != null
		assert field.type == Date
		
		// Test load TestContainerExpanderCollection
		
		ext.load(TestContainerExpanderCollection)
		
		fields = ext.expandedClasses.get(TestContainer)
		
		assert fields.size() == 2
		
		field = fields.find {it.name == 'embedded'}
				
		assert field != null
		assert field.type == List
		
		field = fields.find {it.name == 'reference'}
		
		assert field != null
		assert field.type == List 
		
		// Test load TestContainerExpander
		
		ext.load(TestContainerExpander)
		
		fields = ext.expandedClasses.get(TestContainer)
		
		assert fields.size() == 2
		
		field = fields.find {it.name == 'embedded'}
				
		assert field != null
		assert field.type == TestEmbedded
		
		field = fields.find {it.name == 'reference'}
		
		assert field != null
		assert field.type == TestReference


		
	}
	
	/*
	 * Test loading and saving of primitive metaClass props
	 */
	@Test
	void testAddMetaClassPropsPrimitive() {
		
		log.info "Testing metaClass primitive types"
		
		GroovyMetaClassExtension ext = new GroovyMetaClassExtension(morphia)
		ext.load(TestContainerExpanderPrimitive)

		TestContainer tc = new TestContainer()
		tc.metaClass.num = 78
		tc.metaClass.string = "string"
		tc.metaClass.date = new Date()
		
		ds.save(tc)
		
		TestContainer loaded = ds.get(TestContainer, tc.id)
		
		assert loaded.num == tc.num
		assert loaded.string == tc.string
		assert loaded.date == tc.date
	}
	
	/*
	 * Test default values of primitive metaClass props
	 */
	@Test
	void testAddMetaClassPropsPrimitiveDefaults() {
		
		log.info "Testing metaClass primitive default values"
		
		GroovyMetaClassExtension ext = new GroovyMetaClassExtension(morphia)
		ext.load(TestContainerExpanderPrimitive)

		TestContainer tc = new TestContainer()
		
		TestContainerExpanderPrimitive defs = new TestContainerExpanderPrimitive()
		
		assert tc.num == defs.num
		assert tc.string == defs.string
		assert tc.date == defs.date
		
	}

	
	/*
	 * Test loading and saving of a single @Referenced and @Embedded object
	 */
	@Test
	void testAddMetaClassPropsSingle() {

		log.info "Testing metaClass single Entity or Embedded types"
		
		GroovyMetaClassExtension ext = new GroovyMetaClassExtension(morphia)
		ext.load(TestContainerExpander)

		TestContainer tc = new TestContainer()

		tc.metaClass.reference = new TestReference()
		tc.reference.value = "REF VALUE"


		tc.metaClass.embedded = new TestEmbedded()
		tc.embedded.value = "EMBED VALUE"

		ds.save(tc.reference)
		ds.save(tc)

		TestContainer loaded = ds.get(TestContainer, tc.id)

		assert loaded.reference.value == tc.reference.value
		assert loaded.embedded.value == tc.embedded.value
		
		// Check the db collections to ensure that only the container and the
		// referenced type have collections (the embedded object should not
		// have a collection made for it).
		Set collectionNames = db.getCollectionNames()
		
		assert collectionNames.contains('TestContainer')
		assert collectionNames.contains('TestReference')
		assert !collectionNames.contains('TestEmbedded')

	}

	@Test
	void testAddMetaClassPropsCollection() {

		log.info "Testing metaClass collection types"
		
		GroovyMetaClassExtension ext = new GroovyMetaClassExtension(morphia)
		ext.load(TestContainerExpanderCollection)

		// create TestReference objects
		TestReference tr1 = new TestReference()
		tr1.value = "REF1 VALUE"

		TestReference tr2 = new TestReference()
		tr2.value = "REF2 VALUE"

		TestReference tr3 = new TestReference()
		tr3.value = "REF3 VALUE"

		// create TestEmbedded objects
		TestEmbedded te1 = new TestEmbedded()
		te1.value = "EMB1 VALUE"

		TestEmbedded te2 = new TestEmbedded()
		te2.value = "EMB2 VALUE"

		TestEmbedded te3 = new TestEmbedded()
		te3.value = "EMB3 VALUE"


		TestContainer tc = new TestContainer()

		tc.metaClass.reference = [tr1,tr2,tr3]

		tc.metaClass.embedded = [te1,te2,te3]

		ds.save(tc.reference)
		ds.save(tc)

		TestContainer loaded = ds.get(TestContainer, tc.id)

		//log.info "${loaded.embedded.value}"
		//log.info "${loaded.reference.value}"

		List refs = loaded.reference.inject([]) { acc, val ->
			acc += val.id; acc
		}

		assert refs.size() == 3
		assert refs - [tr1.id,tr2.id,tr3.id] == []

		List emb = loaded.embedded.inject([]) { acc, val ->
			acc += val.value; acc
		}

		assert emb.size() == 3
		assert emb - [te1.value,te2.value,te3.value] == []
		
		// Check the db collections to ensure that only the container and the
		// referenced type have collections (the embedded object should not
		// have a collection made for it).
		Set collectionNames = db.getCollectionNames()
		
		assert collectionNames.contains('TestContainer')
		assert collectionNames.contains('TestReference')		
		assert !collectionNames.contains('TestEmbedded')
		
	}

}
