
 An extension for Morhpia that allows users to specify metaClass expanded fields on
 Entity and Embedded classes. The challenge here was two-fold: 1) morphia is 
 natively java, and as such had not conception of the metaClass, which was effectively
 ignored; and 2) metaClass added fields did not have any associated annotations, so that
 morphia had no way of knowing if the dynamicly added properties should be stored as
 references or embedded.
 
 This was solved by creating a MappedMetaClassField that is metaClass aware and is used
 instead of the MappedField type. This is injected into the MappedClass for the 
 type by injecting an addMappedField class. The problem of reference or embedded annotations
 was solved by creating prototype expansion classes that contain the annotated fields as well
 as a class annotation whose value is the class to expand. This way we know what class
 the fields should be injected into as well as all necessary type and meta information
 required by morphia to save and load them.
 
 To use:
 - new GroovyMetaClassExtension(morphia) must be called early in the init process
 - groovyMetaClassExtension.load(prototype to load) must be called on each prototype class
 