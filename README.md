
An extension for Morhpia that allows users to specify metaClass expanded fields on
Entity and Embedded classes. 

For example:

	@Entity
	class BlogPost {
	
		@Reference User user
		String content
	
	}

Now we would like to add comments to the BlogPost at runtime in a plugin architecture
via the groovy expando-metaclass. How can we do this?

We define the comment class (this is just a normal Morphia entity class):

	@Entity
	class Comment {
	
		@Reference User commenter
		String text
	
	}

And then define the expander class. The class itself here is arbitrary; what's
important is the @ClassToExpand(BlogPost) which marks the class as an expander class 
and tells the extension which class we're expading. The fields contained within, 
which are annotated just as in a normal Morphia class, define the fields we will
inject within the expanded class, in this case, a BlogPost:

	@ClassToExpand(BlogPost) 
	class BlogPostExpander { // name here is arbitrary
	
		List<Comment> comments
	
	}

Default values can also be specified, as well as embedded and referenced types:

	@ClassToExpand(BlogPost) 
	class BlogPostExpander { // name here is arbitrary
	
		@Embedded
		EmbeddedType prop = null
	
		@Reference
		ReferencedTyep prop
	
		int someVal = 5
	
		String aName = "A name"
	
	}



Somewhere in our initialization (after mongo and morphia have been initialized):

	GroovyMetaClassExtension ext = new GroovyMetaClassExtension(morphia) // initialize the extension (tells morphia about its existence)
	ext.load(BlogPostExpander) // tell the extension about the expander class

Now, whenever a BlogPost is saved or loaded, the comments property will automatically be
inserted into the metaclass and saved and loaded along with the BlogPost class (in the
same database table).


---

Implementation:

The challenge here was two-fold: 
1) morphia is natively java, and as such had not conception of the metaClass, which was effectively
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
