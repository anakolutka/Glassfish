/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package javax.enterprise.event;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that a parameter of a method of a bean
 * implementation class is the event parameter
 * of an observer method.
 * 
 * @author Gavin King
 * @author Pete Muir
 * @author David Allen
 */

@Target(PARAMETER)
@Retention(RUNTIME)
@Documented
public @interface Observes
{
   /**
    * Specifies when an observer method should be notified of an event.
    * Defaults to ALWAYS meaning that if a bean instance with the observer
    * method does not already exist, one will be created to receive the
    * event.
    */
	public Notify notifyObserver() default Notify.ALWAYS;
	
	/**
	 * Specifies whether or not the notification should occur as part of
	 * an ongoing transaction, and if so, in which phase of the transaction
	 * the notification should occur.  The default is IN_PROGRESS meaning
	 * the notification is not transactional.
	 */
	public TransactionPhase during() default TransactionPhase.IN_PROGRESS;
}
