package com.github.asm0dey.cligen;

import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Analyzes field types and generates appropriate conversion code.
 */
public class FieldTypeAnalyzer {
    
    public static TypeName getTypeName(TypeMirror mirror) {
        return TypeName.get(mirror);
    }
    
    public static String getConversionCode(String target, String varName, String argValue, TypeMirror type) {
        TypeKind kind = type.getKind();
        String prefix = (target == null || target.isEmpty()) ? "" : target + ".";
        
        switch (kind) {
            case INT:
                return String.format("%s%s = Integer.parseInt(%s)", prefix, varName, argValue);
            case LONG:
                return String.format("%s%s = Long.parseLong(%s)", prefix, varName, argValue);
            case BOOLEAN:
                return String.format("%s%s = Boolean.parseBoolean(%s)", prefix, varName, argValue);
            case DOUBLE:
                return String.format("%s%s = Double.parseDouble(%s)", prefix, varName, argValue);
            case FLOAT:
                return String.format("%s%s = Float.parseFloat(%s)", prefix, varName, argValue);
            default:
                // Default to String assignment
                return String.format("%s%s = %s", prefix, varName, argValue);
        }
    }

    public static String getConversionCode(String varName, String argValue, TypeMirror type) {
        return getConversionCode("instance", varName, argValue, type);
    }

}
