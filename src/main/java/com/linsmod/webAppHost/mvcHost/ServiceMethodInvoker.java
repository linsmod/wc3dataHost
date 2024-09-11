package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.Strings;
import com.linsmod.webAppHost.ann.ServiceMethod;
import com.linsmod.webAppHost.httpserver.Payload;
import com.linsmod.webAppHost.httpserver.CachedFile;

import java.lang.reflect.*;
import java.util.*;

public class ServiceMethodInvoker {
    private final Map<String, Method> methodMap;
    private final Map<String, Method> inherits = new HashMap<>();
    private final ProtoHost handler;

    public ServiceMethodInvoker(ProtoHost handler) {
        this.methodMap = new HashMap<>();
        for (Method method : handler.getClass().getMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                if (!method.isAnnotationPresent(ServiceMethod.class)) continue;
                try {
                    Method declared = handler.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
                    methodMap.put(declared.getName(), declared);
                } catch (NoSuchMethodException e) {
                    inherits.put(method.getName(), method);
                }
            }
        }
        this.handler = handler;
    }

    public boolean methodExists(String methodName) {
        Objects.requireNonNull(methodName, "Method name can not be null.");
        return methodMap.containsKey(methodName);
    }

    /**
     * @param methodName
     * @param payloads
     * @return
     */
    public Object invoke(String methodName, Payload[] payloads) throws Throwable {
        if (methodName == null) {
            throw new IllegalArgumentException("Null method is not allowed");
        }
        Method method = methodMap.get(methodName);
        if (method == null)
            method = inherits.get(methodName);
//        if (method == null)
//            method = methodMap.get("files"); // fallback to files
        if (method == null) {
            throw new NoSuchMethodException("No such method in service： " + methodName);
        }
        Object[] arguments = null;
        try {
            arguments = createArgumentArray(method, payloads);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Can not decode calling arguments from payloads", ex);
        }
        Throwable exception = null;
        try {
            Result authResult = callOnAuth(method);
            if (authResult != null) {
                return authResult;
            }
            Object returnObject = method.invoke(handler, arguments);
            if (method.getReturnType() == void.class) {
                returnObject = Result.Void;
            } else if (returnObject == null) {
                returnObject = Result.Null;
            }
            if (returnObject instanceof Result) {
                return returnObject;
            }
            return Result.json(returnObject);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            cause.printStackTrace();
            return new Result.ErrorResult(cause, 500);
        }
    }

    public Result callOnAuth(Method target) throws Throwable {
        ServiceMethod annotation = target.getAnnotation(ServiceMethod.class);
        if (annotation == null || annotation.authRequired()) {
            Method onAuth = methodMap.get("onAuth");
            if (onAuth == null) onAuth = inherits.get("onAuth");
            if (onAuth == null) {
                throw new RuntimeException("Service not configure onAuth method!");
            }
            try {
                Object result = onAuth.invoke(handler);
                if (result == null) {
                    return null;
                }
                if (result instanceof Result) {
                    return (Result) result;
                } else {
                    return Result.error("Service onAuth delegate returns unexpected data");
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
        return null;
    }

    private Object[] createSingleElementArgumentArray(Method method, ProtoArgument protoArgument, Payload[] payloads) {
        Objects.requireNonNull(method);
        Parameter[] parameters = method.getParameters();
        assert parameters.length == 1;
        SingleArgumentVisitor argument = new SingleArgumentVisitor(protoArgument);
        argument.visit(payloads);
        return new Object[]{argument.getValue()};
    }

    private Object accessObjectField(Object obj, String field) throws IllegalAccessException {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(field);
        for (Field declaredField : obj.getClass().getDeclaredFields()) {
            if (declaredField.getName().equalsIgnoreCase(field)) {
                return declaredField.get(obj);
            }
        }
        return null;
    }

    private Object[] createArgumentArray(Method method, Payload[] payloads) {
        Objects.requireNonNull(method);
        ServiceMethod annotation = method.getAnnotation(ServiceMethod.class);
        List<ProtoArgument> arguments = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        if (parameters.length > 0 && annotation == null) {
            if (parameters.length == 1) {
                if (parameters[0].getType() != CachedFile.class && !parameters[0].getType().isArray()) {
                    if (parameters.length == 1)
                        return createSingleElementArgumentArray(method, new ProtoArgument(null, parameters[0].getType()), payloads);
                    throw new RuntimeException("内部错误:ServiceMethod必须被定义，在" + method);
                }
            } else throw new RuntimeException("内部错误:ServiceMethod必须被定义，在" + method);
        }
        if (annotation != null && !Strings.isNullOrEmpty(annotation.argNames())) {
            String[] names = annotation.argNames().split(",");
            if (names.length != parameters.length) {
                throw new RuntimeException("内部错误:ServiceMethod指定的参数数目与方法实现必须一致。在" + method.getName());
            }
            for (int i = 0; i < names.length; i++) {
                if (names[i].trim().equals(""))
                    throw new RuntimeException("内部错误:ServiceMethod参数名不准空值。在" + method.getName());
                ProtoArgument protoArgument = new ProtoArgument(names[i], parameters[i].getType());
                arguments.add(protoArgument);
            }
        }

        if (parameters.length == 0) {
            return new Object[0];
        }
        if (parameters.length == 1) {
            if (arguments.size() == 1) {
                return createSingleElementArgumentArray(method, arguments.get(0), payloads);
            } else {
                return createSingleElementArgumentArray(method, new ProtoArgument(null, parameters[0].getType()), payloads);
            }
        }
        ArgumentsVisitor visitor = new ArgumentsVisitor(arguments);
        return visitor.visit(payloads);
    }
}