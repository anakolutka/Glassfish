/**
 * 
 */
package org.jboss.webbeans.el;

import org.jboss.webbeans.context.DependentContext;
import org.jboss.webbeans.context.DependentInstancesStore;
import org.jboss.webbeans.context.DependentStorageRequest;

abstract class RunInDependentContext
{
   
   private final DependentStorageRequest dependentStorageRequest;
   
   public RunInDependentContext()
   {
      dependentStorageRequest = DependentStorageRequest.of(new DependentInstancesStore(), new Object());
   }
   
   private void setup()
   {
      DependentContext.instance().setActive(true);
      DependentContext.instance().startCollectingDependents(dependentStorageRequest);
   }
   
   private void cleanup()
   {
      DependentContext.instance().stopCollectingDependents(dependentStorageRequest);
      // TODO kinky
      dependentStorageRequest.getDependentInstancesStore().destroyDependentInstances(dependentStorageRequest.getKey());
      DependentContext.instance().setActive(false);
   }
   
   protected abstract void execute() throws Exception;
   
   public final void run() throws Exception
   {
      try
      {
         setup();
         execute();
      }
      finally
      {
         cleanup();
      }
   }
   
}