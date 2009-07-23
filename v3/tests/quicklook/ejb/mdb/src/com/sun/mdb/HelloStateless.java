package com.sun.mdb;

import javax.ejb.Stateless;

// Hello1 interface is not annotated with @Local. If the
// bean only implements one interface it is assumed to be
// a local business interface.
@Stateless(description="my stateless bean description") 
public class HelloStateless implements Hello1 {

    public void hello(String s) {
        System.out.println("HelloStateless: " + s);
    }

}
