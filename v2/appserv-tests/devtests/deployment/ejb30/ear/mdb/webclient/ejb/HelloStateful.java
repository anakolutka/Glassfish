package com.sun.s1asdev.ejb.ejb30.hello.mdb;

import javax.ejb.Stateful;
import javax.ejb.Remove;
import javax.annotation.PreDestroy;

// Hello1 interface is not annotated with @Local. If the
// bean only implements one interface it is assumed to be
// a local business interface.
@Stateful public class HelloStateful implements Hello2 {

    private String msg;

    public void hello(String s) {
        msg = s;
        System.out.println("HelloStateful: " + s);
    }

    @Remove public void removeMethod() {
        System.out.println("Business method marked with @Remove called in " +
                           msg);
    }
    @PreDestroy public void myPreDestroyMethod() {
        System.out.println("PRE-DESTROY callback received in " + msg);        
    }

}
