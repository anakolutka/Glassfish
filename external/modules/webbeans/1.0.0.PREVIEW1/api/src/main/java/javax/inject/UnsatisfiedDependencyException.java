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


package javax.inject;

/**
 * Thrown if no beans can be resolved
 * 
 * @author Pete Muir
 */
public class UnsatisfiedDependencyException extends DeploymentException
{

   private static final long serialVersionUID = 5350603312442756709L;

   public UnsatisfiedDependencyException()
   {
      super();
   }

   public UnsatisfiedDependencyException(String message, Throwable throwable)
   {
      super(message, throwable);
   }

   public UnsatisfiedDependencyException(String message)
   {
      super(message);
   }

   public UnsatisfiedDependencyException(Throwable throwable)
   {
      super(throwable);
   }

   

}
