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
    
    public static String getConversionCode(String varName, String argValue, TypeMirror type) {
        TypeKind kind = type.getKind();
        
        switch (kind) {
            case INT:
                return String.format("instance.%s = Integer.parseInt(%s)", varName, argValue);
            case LONG:
                return String.format("instance.%s = Long.parseLong(%s)", varName, argValue);
            case BOOLEAN:
                return String.format("instance.%s = Boolean.parseBoolean(%s)", varName, argValue);
            case DOUBLE:
                return String.format("instance.%s = Double.parseDouble(%s)", varName, argValue);
            case FLOAT:
                return String.format("instance.%s = Float.parseFloat(%s)", varName, argValue);
            default:
                // Default to String assignment
                return String.format("instance.%s = %s", varName, argValue);
        }
    }

}
