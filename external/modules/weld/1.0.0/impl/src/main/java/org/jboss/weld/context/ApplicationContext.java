package org.jboss.weld.context;

import javax.enterprise.context.ApplicationScoped;

public class ApplicationContext extends AbstractApplicationContext
{
   
   public ApplicationContext()
   {
      super(ApplicationScoped.class);
   }

}
