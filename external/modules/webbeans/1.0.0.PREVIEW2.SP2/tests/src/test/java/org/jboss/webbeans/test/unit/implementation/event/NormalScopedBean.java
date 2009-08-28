
package org.jboss.webbeans.test.unit.implementation.event;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;

@SessionScoped
public class NormalScopedBean implements Serializable
{

   @Any Event<Foo> event;
   
}
