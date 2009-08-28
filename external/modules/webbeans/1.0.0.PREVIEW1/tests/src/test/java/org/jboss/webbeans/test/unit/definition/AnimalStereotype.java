package org.jboss.webbeans.test.unit.definition;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.annotation.Stereotype;
import javax.context.RequestScoped;

@Stereotype(requiredTypes=Animal.class)
@Target( { TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@RequestScoped
@interface AnimalStereotype
{

}
