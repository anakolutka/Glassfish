/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.s1asdev.ejb.ejb30.interceptors.session;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;
import javax.interceptor.AroundInvoke;

public class ErrorRecoveryInterceptor {

    @AroundInvoke
    public Object interceptCall(InvocationContext ctx)
   	throws MyBadException, AssertionFailedException, Exception 
    {
        Method method = ctx.getMethod();
        String methodName = method.getName();
        Object[] params = ctx.getParameters();
        boolean recoverFromError = false;
        Object retVal = ctx.proceed();

        if (methodName.equals("assertIfTrue")) {
            params[0] = new Boolean(false);
 
            try {
                ctx.setParameters(params);
            } catch (IllegalArgumentException illArgEx) {
                throw new MyBadException("Invalid type as argument", illArgEx);
            }

            retVal = ctx.proceed();
        }

        return retVal;
    }

}
