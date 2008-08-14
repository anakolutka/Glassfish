package org.glassfish.flashlight.impl.core;

/**
 * @author Mahesh Kannan
 *         Date: Jul 20, 2008
 */

import org.glassfish.flashlight.provider.Probe;
import org.glassfish.flashlight.provider.ProbeRegistry;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;
import java.security.ProtectionDomain;

public class ProviderImplGenerator {

    public String defineClass(ProbeProvider provider, Class providerClazz) {

        String generatedClassName = provider.getModuleName() + "_Flashlight_" + provider.getProviderName() + "_"
                + "App_" + ((provider.getAppName() == null) ? "" : provider.getAppName());
        generatedClassName = providerClazz.getName() + "_" + generatedClassName;

        byte[] classData = generateClassData(provider, providerClazz, generatedClassName);

        ProtectionDomain pd = providerClazz.getProtectionDomain();
        byte[] dataType = new byte[0];

        java.lang.reflect.Method jm = null;
        for (java.lang.reflect.Method jm2 : ClassLoader.class.getDeclaredMethods()) {
            if (jm2.getName().equals("defineClass") && jm2.getParameterTypes().length == 5) {
                jm = jm2;
                break;
            }
        }

        final java.lang.reflect.Method clM = jm;
        try {
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            if (!clM.isAccessible()) {
                                clM.setAccessible(true);
                            }
                            return null;
                        }
                    });

            clM.invoke(providerClazz.getClassLoader(), generatedClassName, classData, 0,
                    classData.length, pd);

            return generatedClassName;
        } catch (PrivilegedActionException pEx) {
            throw new RuntimeException(pEx);
        } catch (IllegalAccessException
                illegalAccessException) {
            throw new RuntimeException(illegalAccessException);
        } catch (InvocationTargetException
                invtEx) {
            throw new RuntimeException(invtEx);
        }

    }


    public byte[] generateClassData(ProbeProvider provider, Class providerClazz, String generatedClassName) {

        Type classType = Type.getType(providerClazz);
        //System.out.println("** classType: " + classType);
        //System.out.println("** classDesc: " + Type.getDescriptor(providerClazz));

        //System.out.println("Generating for: " + generatedClassName);

        generatedClassName = generatedClassName.replace('.', '/');

        int cwFlags = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;
        ClassWriter cw = new ClassWriter(cwFlags);

        int access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL;
        String[] interfaces = new String[]{providerClazz.getName().replace('.', '/')};
        cw.visit(Opcodes.V1_5, access, generatedClassName, null, "java/lang/Object", interfaces);


        for (Probe probe : provider.getProbes()) {
            Type probeType = Type.getType(Probe.class);
            int fieldAccess = Opcodes.ACC_PUBLIC;
            String fieldName = "_flashlight_" + probe.getProbeName();
            cw.visitField(fieldAccess, fieldName,
                    probeType.getDescriptor(), null, null);
        }

        Type probeType = Type.getType(Probe.class);
        for (Probe probe : provider.getProbes()) {
            String methodDesc = "void " + probe.getProviderJavaMethodName();
            methodDesc += "(";
            String delim = "";
            for (Class paramType : probe.getParamTypes()) {
                methodDesc += delim + paramType.getName();
                delim = ", ";
            }
            methodDesc += ")";
            Method m = Method.getMethod(methodDesc);
            GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, null, cw);

            String fieldName = "_flashlight_" + probe.getProbeName();
            gen.loadThis();
            gen.visitFieldInsn(Opcodes.GETFIELD,
                    generatedClassName,
                    fieldName, probeType.getDescriptor());
            int index = gen.newLocal(probeType);
            gen.storeLocal(index);
            gen.loadLocal(index);
            gen.invokeVirtual(probeType, Method.getMethod("boolean isEnabled()"));
            gen.push(true);
            Label enabledLabel = new Label();
            Label notEnabledLabel = new Label();

            gen.ifCmp(Type.getType(boolean.class), GeneratorAdapter.EQ, enabledLabel);
            gen.goTo(notEnabledLabel);
            gen.visitLabel(enabledLabel);
            gen.loadLocal(index);
            gen.loadArgArray();
            gen.invokeVirtual(probeType, Method.getMethod("void fireProbe(Object[])"));
            gen.visitLabel(notEnabledLabel);
            gen.returnValue();
            gen.endMethod();
        }


        generateConstructor(cw, generatedClassName, provider);

        cw.visitEnd();

        byte[] classData = cw.toByteArray();

        return classData;
    }

    private void generateConstructor(ClassWriter cw, String generatedClassName, ProbeProvider provider) {
        Method m = Method.getMethod("void <init> ()");
        GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, null, cw);
        gen.loadThis();
        gen.invokeConstructor(Type.getType(Object.class), m);

        Type probeRegType = Type.getType(ProbeRegistry.class);
        Type probeType = Type.getType(Probe.class);
        
        gen.loadThis();
        for (Probe probe : provider.getProbes()) {

            gen.dup();
            
            String fieldName = "_flashlight_" + probe.getProbeName();
            gen.push(probe.getId());
            gen.invokeStatic(probeRegType,
                Method.getMethod("org.glassfish.flashlight.provider.Probe getProbeById(int)"));

            gen.visitFieldInsn(Opcodes.PUTFIELD,
                    generatedClassName,
                    fieldName, probeType.getDescriptor());
        }

        gen.pop();
        //return the value from constructor
        gen.returnValue();
        gen.endMethod();
    }

    private void generateSystemOutPrintln(GeneratorAdapter mg, String msg) {
        mg.getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));
        mg.push(msg);
        mg.invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (String)"));
        mg.returnValue();
    }
}
