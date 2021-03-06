/*
 * Copyright  2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.jsp.compilers;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.jsp.JspNameMangler;
import org.apache.tools.ant.taskdefs.optional.jsp.Jasper41Mangler;


/**
 * Creates the necessary compiler adapter, given basic criteria.
 *
 */
public class JspCompilerAdapterFactory {

    /** This is a singleton -- can't create instances!! */
    private JspCompilerAdapterFactory() {
    }

    /**
     * Based on the parameter passed in, this method creates the necessary
     * factory desired.
     *
     * The current mapping for compiler names are as follows:
     * <ul><li>jasper = jasper compiler (the default)
     * <li><i>a fully quallified classname</i> = the name of a jsp compiler
     * adapter
     * </ul>
     *
     * @param compilerType either the name of the desired compiler, or the
     * full classname of the compiler's adapter.
     * @param task a task to log through.
     * @throws BuildException if the compiler type could not be resolved into
     * a compiler adapter.
     */
    public static JspCompilerAdapter getCompiler(String compilerType, Task task)
        throws BuildException {
        return getCompiler(compilerType, task,
                           task.getProject().createClassLoader(null));
    }

    /**
     * Based on the parameter passed in, this method creates the necessary
     * factory desired.
     *
     * The current mapping for compiler names are as follows:
     * <ul><li>jasper = jasper compiler (the default)
     * <li><i>a fully quallified classname</i> = the name of a jsp compiler
     * adapter
     * </ul>
     *
     * @param compilerType either the name of the desired compiler, or the
     * full classname of the compiler's adapter.
     * @param task a task to log through.
     * @param loader AntClassLoader with which the compiler should be loaded
     * @throws BuildException if the compiler type could not be resolved into
     * a compiler adapter.
     */
    public static JspCompilerAdapter getCompiler(String compilerType, Task task,
                                                 AntClassLoader loader)
        throws BuildException {

        if (compilerType.equalsIgnoreCase("jasper")) {
            //tomcat4.0 gets the old mangler
            return new JasperC(new JspNameMangler());
        }
        if (compilerType.equalsIgnoreCase("jasper41")) {
            //tomcat4.1 gets the new one
            return new JasperC(new Jasper41Mangler());
        }
        return resolveClassName(compilerType, loader);
    }

    /**
     * Tries to resolve the given classname into a compiler adapter.
     * Throws a fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @param classloader Classloader with which to load the class
     * @throws BuildException This is the fit that is thrown if className
     * isn't an instance of JspCompilerAdapter.
     */
    private static JspCompilerAdapter resolveClassName(String className,
                                                       AntClassLoader classloader)
        throws BuildException {
        try {
            Class c = classloader.findClass(className);
            Object o = c.newInstance();
            return (JspCompilerAdapter) o;
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException(className + " can\'t be found.", cnfe);
        } catch (ClassCastException cce) {
            throw new BuildException(className + " isn\'t the classname of "
                                     + "a compiler adapter.", cce);
        } catch (Throwable t) {
            // for all other possibilities
            throw new BuildException(className + " caused an interesting "
                                     + "exception.", t);
        }
    }

}
