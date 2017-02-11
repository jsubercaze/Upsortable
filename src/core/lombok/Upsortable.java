package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lombok.Setter.AnyAnnotation;

@Target(ElementType.TYPE) @Retention(RetentionPolicy.SOURCE) public @interface Upsortable {
	/**
	 * If you want your setter to be non-public, you can specify an alternate
	 * access level here.
	 */
	lombok.AccessLevel value() default lombok.AccessLevel.PUBLIC;
	
	/**
	 * Any annotations listed here are put on the generated method. The syntax
	 * for this feature is: {@code @Setter(onMethod=@__({@AnnotationsGoHere}))}
	 */
	AnyAnnotation[] onMethod() default {};
	
	/**
	 * Any annotations listed here are put on the generated method's parameter.
	 * The syntax for this feature is:
	 * {@code @Setter(onParam=@__({@AnnotationsGoHere}))}
	 */
	AnyAnnotation[] onParam() default {};
}