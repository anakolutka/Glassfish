package com.acme;

import javax.ejb.*;
import javax.annotation.*;
import javax.interceptor.*;

@Singleton
//@Startup
public class SingletonBean extends BaseBean implements Snglt {


    @EJB Sful sful;

    @PostConstruct
    public void init() {
        System.out.println("In SingletonBean::init()");
        verifyMethod("init");
        sful.hello();
    }
    
    public String hello() {
        verify("SingletonBean");
	System.out.println("In SingletonBean::hello()");
        sful.remove();
	return "hello, world!\n";
    }

    @PreDestroy
    public void destroy() {
        System.out.println("In SingletonBean::destroy()");
        verifyMethod("destroy");
    }
}
