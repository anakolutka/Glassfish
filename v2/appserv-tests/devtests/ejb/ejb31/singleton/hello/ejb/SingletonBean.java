package com.acme;

import javax.ejb.*;
import javax.annotation.*;
import org.omg.CORBA.ORB;

import javax.naming.InitialContext;

import javax.management.j2ee.ManagementHome;

@Singleton
@Remote(Hello.class)
    @EJB(name="mejb", beanInterface=javax.management.j2ee.ManagementHome.class, mappedName="ejb/mgmt/MEJB")
@Startup
public class SingletonBean {

    @Resource
    private ORB orb;

    /*Object returned from IIOP_OBJECT_FACTORY is still ior
    @EJB(mappedName="ejb/mgmt/MEJB")
    ManagementHome mHome1;
    */

    /* Object returned from IIOP_OBJECT_FACTORY is still ior
    @EJB(lookup="java:global/mejb/MEJBBean")
    ManagementHome mHome2;
    */

    /* Doesn't work b/c actual MEJB app Home interface is new glassfish
     * type, so actual type derived from field declaration is tacked
     * onto mappedName and results in NameNotFound
    @EJB(mappedName="java:global/mejb/MEJBBean")
    ManagementHome mHome2;
    */

    


    //MEJBHome mHome2;

    @PostConstruct
    public void init() {
        System.out.println("In SingletonBean::init()");
        System.out.println("orb = " + orb);
	if( orb == null ) {
	    throw new EJBException("null ORB");
	}
	try {
	    // same problem ManagementHome mHomeL = (ManagementHome) new InitialContext().lookup("java:comp/env/mejb");
	    // same problem ManagementHome mHomeL2 = (ManagementHome) new InitialContext().lookup("java:global/mejb/MEJBBean");
	    // System.out.println("mHomeL = " + mHomeL);
	    // System.out.println("mHomeL2 = " + mHomeL2);
	} catch(Exception e) {
	    throw new EJBException(e);
	}

	// System.out.println("mHome1 = " + mHome1);
	//	System.out.println("mHome2 = " + mHome2);

    }
    
    public String hello() {
	System.out.println("In SingletonBean::hello()");
	return "hello, world!\n";
    }

    public void testError() {
	throw new Error("test java.lang.Error");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("In SingletonBean::destroy()");
    }



}
