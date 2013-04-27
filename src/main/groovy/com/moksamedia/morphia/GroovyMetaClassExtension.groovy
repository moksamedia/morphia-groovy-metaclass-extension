package com.moksamedia.morphia

import groovy.util.logging.Slf4j

import java.lang.annotation.Annotation

import com.github.jmkgreen.morphia.AbstractEntityInterceptor
import com.github.jmkgreen.morphia.Morphia
import com.github.jmkgreen.morphia.mapping.MappedClass
import com.github.jmkgreen.morphia.mapping.MappedField
import com.github.jmkgreen.morphia.mapping.Mapper
import com.mongodb.DBObject


/**
 * An extension for Morhpia that allows users to specify metaClass expanded fields on
 * Entity and Embedded classes. The challenge here was two-fold: 1) morphia is 
 * natively java, and as such had not conception of the metaClass, which was effectively
 * ignored; and 2) metaClass added fields did not have any associated annotations, so that
 * morphia had no way of knowing if the dynamicly added properties should be stored as
 * references or embedded.
 * 
 * This was solved by creating a MappedMetaClassField that is metaClass aware and is used
 * instead of the MappedField type. This is injected into the MappedClass for the 
 * type by injecting an addMappedField class. The problem of reference or embedded annotations
 * was solved by creating prototype expansion classes that contain the annotated fields as well
 * as a class annotation whose value is the class to expand. This way we know what class
 * the fields should be injected into as well as all necessary type and meta information
 * required by morphia to save and load them.
 * 
 * To use:
 * - new GroovyMetaClassExtension(morphia) must be called early in the init process
 * - groovyMetaClassExtension.load(prototype to load) must be called on each prototype class
 * 
 * @author cantgetnosleep
 *
 */
@Slf4j
class GroovyMetaClassExtension extends AbstractEntityInterceptor {

	/*
	 * Tracks the classes that have been loaded and the fields added to those
	 * classes, along with the associated annotations.
	 */
	private Map expandedClasses = [:]
	
	/**
	 * This must be called early, once the morphia instance has been created. This
	 * tells the mapper about the extension so that the injection points are
	 * active.
	 * @param morphia
	 */
	public GroovyMetaClassExtension(final Morphia morphia) {
		morphia.getMapper().addInterceptor(this)
	}
	
	/**
	 * Given a class, checks to see if it has the @ClassToExpand annotation,
	 * and if so, returns the class referred to by the value of the annotation. 
	 */
	Closure getClassToExpand = { Class clazz ->
		clazz.getAnnotation(ClassToExpand) != null ? clazz.getAnnotation(ClassToExpand).value() : {
			throw new IllegalArgumentException("${clazz.simpleName} is not annotated with either @ClassToExpand or @ClassNameToExpand.")
		}
	}
	
	/**
	 * Must be called on each expander class, which should be annotated with
	 * ClassToExpand
	 * @param clazz
	 * @return
	 */
	public load(Class clazz) {
				
		Class classToExpand = getClassToExpand(clazz)
	
		List fields = clazz.declaredFields.grep { !it.synthetic } // this gets rid of the groovy injected fields
		
		expandedClasses.put(classToExpand, fields)
		
		// set default values
		def instance = clazz.newInstance()
		fields.each {
			classToExpand.metaClass."${it.name}" = instance."${it.name}"
		}
		
		//log.info "${clazz.simpleName} loaded for ${classToExpand.simpleName}"
		
	}

	/**
	 * This is where the magic happens. 
	 * 1) If the ent.class has already been loaded (thus is in the expandedClasses Map), 
	 *    we get the MappedClass for the type from the mapper. 
	 * 2) We inject a addMappedField method because natively this doesn't exist and we 
	 *    need to be able to add our expanded metaClass fields.
	 * 3) We iterate over the list of expanded fields (retrieved from the Map), and for
	 *    each create a metaClass-aware MappedField (the MappedMetaClassField subclass),
	 *    which we add to the mapped class.
	 * 
	 * We have to do this on prePersist and preLoad, but that is all we need to do to
	 * hook into the morphia framework to allow it to save and load metaClass-expanded
	 * fields.
	 */
	Closure checkClass = { Object ent, Mapper mapr ->
		
		// If we've called load() on a class, it's in the map
		if (expandedClasses.containsKey(ent.class)) {
			
			// Get the mapped class from the morphia mapper
			MappedClass mappedClass = mapr.getMappedClass(ent)
			
			// Inject a ninja method that allows us to add mapped fields to the mapped class
			mappedClass.metaClass.addMappedField = { MappedField toAdd ->
				delegate.mappedFields.add(toAdd)
			}

			// Get the previously stored list of dynamically-added fields
			List fields = expandedClasses[ent.class]

			// For each one...
			fields.each {
	
				// Create a new mapped field (metaClass aware thanks to the 
				// MappedMetaClassField class
				MappedField mf = new MappedMetaClassField(it, ent.class)
				
				// Add it to the mapped class
				mappedClass.addMappedField(mf)
								
			}
			
			// Let Morphia go on about it's business, none the wiser
						
		}

	}
	
	@Override
	public void prePersist(Object ent, DBObject dbObj, Mapper mapr) {		
		//log.info "prePerist called: ${ent.class.simpleName}"
		checkClass(ent,mapr)
	}
	
	@Override
	public void preLoad(Object ent, DBObject dbObj, Mapper mapr) {
		//log.info "preLoad called: ${ent.class.simpleName}"
		checkClass(ent,mapr)
	}

	
}
