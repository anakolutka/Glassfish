package org.jboss.webbeans.test.unit.implementation.producer.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.Produces;

public class ParameterizedProducer
{

   @Produces
   public List<String> createStringList()
   {
      return Arrays.asList("aaa", "bbb");
   }

   @Produces
   public List createList()
   {
      return Arrays.asList(1, 2, 3);
   }

   @Produces
   public ArrayList<Integer> createIntegerList()
   {
      List<Integer> list = Arrays.asList(1, 2, 3, 4);
      ArrayList<Integer> arrayList = new ArrayList<Integer>();
      arrayList.addAll(list);
      return arrayList;
   }
}
