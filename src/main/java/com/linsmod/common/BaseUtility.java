/*
 * Copyright (C)  Tony Green, LitePal Framework Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linsmod.common;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * A utility class to help LitePal with some base actions that might through any
 * components. These actions can help classes just do the jobs they care, and
 * help them out of the trivial work.
 *
 * @author Tony Green
 * @since 1.0
 */
public class BaseUtility {

    /**
     * Disable to create an instance of BaseUtility.
     */
    private BaseUtility() {
    }

    /**
     * This helper method makes up the shortage of contains method in Collection
     * to support the function of case insensitive contains. It only supports
     * the String generic type of collection, cause other types have no cases
     * concept.
     *
     * @param collection The collection contains string data.
     * @param string     The string want to look for in the collection.
     * @return If the string is in the collection without case concern return
     * true, otherwise return false. If the collection is null, return
     * false.
     */
    public static boolean containsIgnoreCases(Collection<String> collection, String string) {
        if (collection == null) {
            return false;
        }
        if (string == null) {
            return collection.contains(null);
        }
        boolean contains = false;
        for (String element : collection) {
            if (string.equalsIgnoreCase(element)) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    /**
     * Judge a field type is supported or not. Currently only basic data types
     * and String are supported.
     *
     * @param fieldType Type of the field.
     * @return Supported return true, not supported return false.
     */
    public static boolean isFieldTypeSupported(String fieldType) {
        if ("boolean".equals(fieldType) || "java.lang.Boolean".equals(fieldType)) {
            return true;
        }
        if ("float".equals(fieldType) || "java.lang.Float".equals(fieldType)) {
            return true;
        }
        if ("double".equals(fieldType) || "java.lang.Double".equals(fieldType)) {
            return true;
        }
        if ("int".equals(fieldType) || "java.lang.Integer".equals(fieldType)) {
            return true;
        }
        if ("long".equals(fieldType) || "java.lang.Long".equals(fieldType)) {
            return true;
        }
        if ("short".equals(fieldType) || "java.lang.Short".equals(fieldType)) {
            return true;
        }
        if ("char".equals(fieldType) || "java.lang.Character".equals(fieldType)) {
            return true;
        }
        if ("java.lang.String".equals(fieldType) || "java.util.Date".equals(fieldType)) {
            return true;
        }
        return false;
    }

    /**
     * Judge a generic type is supported or not. Currently only basic data types
     * and String are supported.
     *
     * @param genericType Type of the generic field.
     * @return Supported return true, not supported return false.
     */
    public static boolean isGenericTypeSupported(String genericType) {
        if ("java.lang.String".equals(genericType)) {
            return true;
        } else if ("java.lang.Integer".equals(genericType)) {
            return true;
        } else if ("java.lang.Float".equals(genericType)) {
            return true;
        } else if ("java.lang.Double".equals(genericType)) {
            return true;
        } else if ("java.lang.Long".equals(genericType)) {
            return true;
        } else if ("java.lang.Short".equals(genericType)) {
            return true;
        } else if ("java.lang.Boolean".equals(genericType)) {
            return true;
        } else if ("java.lang.Character".equals(genericType)) {
            return true;
        }
        return false;
    }

    /**
     * Check the existence of the specific class and method.
     *
     * @param className  Class name with full package name.
     * @param methodName Method name.
     * @return Return true if both of class and method are exist. Otherwise return false.
     */
    public static boolean isClassAndMethodExist(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
