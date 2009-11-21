/*
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections;

import junit.framework.*;

/**
 * Extension of {@link TestBag} for exercising the {@link HashBag}
 * implementation.
 *
 * @author Chuck Burdick
 * @version $Id: TestHashBag.java,v 1.1.4.1 2004/05/22 12:14:05 scolebourne Exp $ */
public class TestHashBag extends TestBag {
   public TestHashBag(String testName) {
      super(testName);
   }

   public static Test suite() {
      return new TestSuite(TestHashBag.class);
   }

   public static void main(String args[]) {
      String[] testCaseName = { TestHashBag.class.getName() };
      junit.textui.TestRunner.main(testCaseName);
   }

   public Bag makeBag() {
      return new HashBag();
   }
}

