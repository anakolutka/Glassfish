package com.acme;

import javax.ejb.*;
import javax.annotation.*;
import javax.interceptor.*;

import javax.inject.Inject;

import javax.naming.InitialContext;

import javax.enterprise.inject.spi.BeanManager;

import java.lang.reflect.Method;

import org.jboss.weld.examples.translator.*;

@Singleton
@Startup
public class SingletonBean implements SingletonRemote {

    @Inject Foo foo;

    @EJB
	private StatelessLocal statelessEE;

    @EJB private TranslatorController tc;

    @Resource(lookup="java:module/FooManagedBean")
    private FooManagedBean fmb;

    @Resource
    private BeanManager beanManagerInject;
    
    @Resource SessionContext sesCtx;

    @PostConstruct
    public void init() {
        System.out.println("In SingletonBean::init()");
	if( beanManagerInject == null ) {
	    throw new EJBException("BeanManager is null");
	}
	System.out.println("Bean manager inject = " + beanManagerInject);
	testIMCreateDestroyMO();
    }

    public void hello() {
	System.out.println("In SingletonBean::hello() " + foo);
	statelessEE.hello();

	fmb.hello();
	
	BeanManager beanMgr = (BeanManager)
	    sesCtx.lookup("java:comp/BeanManager");
	
	System.out.println("Successfully retrieved bean manager " +
			   beanMgr + " for JCDI enabled app");
			   
    }

    @Schedule(second="*/10", minute="*", hour="*")
	private void timeout() {
	System.out.println("In SingletonBean::timeout() " + foo);
	statelessEE.hello();
    }

    @PreDestroy
    public void destroy() {
        System.out.println("In SingletonBean::destroy()");
    }

    private void testIMCreateDestroyMO() {

	try {

	    // Test InjectionManager managed bean functionality
	    Object injectionMgr = new InitialContext().lookup("com.sun.enterprise.container.common.spi.util.InjectionManager");
	    Method createManagedMethod = injectionMgr.getClass().getMethod("createManagedObject", java.lang.Class.class);
	    System.out.println("create managed object method = " + createManagedMethod);
	    FooManagedBean f2 = (FooManagedBean) createManagedMethod.invoke(injectionMgr, FooManagedBean.class);
	    f2.hello();
	

	    Method destroyManagedMethod = injectionMgr.getClass().getMethod("destroyManagedObject", java.lang.Object.class);
	    System.out.println("destroy managed object method = " + destroyManagedMethod);
	    destroyManagedMethod.invoke(injectionMgr, f2);

	     FooNonManagedBean nonF = (FooNonManagedBean) createManagedMethod.invoke(injectionMgr, FooNonManagedBean.class);
	     System.out.println("FooNonManagedBean = " + nonF);
	     nonF.hello();
	     destroyManagedMethod.invoke(injectionMgr, nonF);


	} catch(Exception e) {
	    throw new EJBException(e);
	}


    }



}
