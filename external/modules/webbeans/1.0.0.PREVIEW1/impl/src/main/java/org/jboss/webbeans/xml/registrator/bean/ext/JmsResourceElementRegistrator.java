package org.jboss.webbeans.xml.registrator.bean.ext;

import org.dom4j.Element;
import org.jboss.webbeans.introspector.AnnotatedClass;
import org.jboss.webbeans.xml.ParseXmlHelper;
import org.jboss.webbeans.xml.XmlConstants;
import org.jboss.webbeans.xml.checker.beanchildren.BeanChildrenChecker;

public class JmsResourceElementRegistrator extends NotSimpleBeanElementRegistrator
{
   public JmsResourceElementRegistrator(BeanChildrenChecker childrenChecker)
   {
      super(childrenChecker);
   }

   public boolean accept(Element beanElement, AnnotatedClass<?> beanClass)
   {
      if (ParseXmlHelper.isJavaEeNamespace(beanElement) && 
            (beanElement.getName().equalsIgnoreCase(XmlConstants.TOPIC) || 
                  beanElement.getName().equalsIgnoreCase(XmlConstants.QUEUE)))
         return true;
      return false;
   }
}
