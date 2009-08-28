package javax.webbeans;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.annotation.Named;
import javax.annotation.Stereotype;
import javax.context.RequestScoped;

/**
 * A stereotype for MVC model objects
 * 
 * @author Gavin King
 */

@Named
@RequestScoped
@Stereotype
@Target( { TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface Model
{
}