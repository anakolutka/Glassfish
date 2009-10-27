package com.sun.org.apache.xml.internal.security.test.utils.resolver;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import com.sun.org.apache.xml.internal.security.Init;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolver;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ResolverDirectHTTP extends TestCase {
  public void testBug40783() throws Exception{
	  Init.init();
	  Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();		
	  Attr uri=doc.createAttribute("id");
	  uri.setNodeValue("urn:ddd:uuu");
	  ((Element)doc.createElement("test")).setAttributeNode(uri);
	  try {
		  ResourceResolver resolver=ResourceResolver.getInstance(uri, null);		  
		  fail("No exception throw, but resolver found:"+resolver);
	  } catch (ResourceResolverException e) {
		  
	  }
	}
  
}
