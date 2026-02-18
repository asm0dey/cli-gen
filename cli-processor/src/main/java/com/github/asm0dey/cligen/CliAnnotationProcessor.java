package com.github.asm0dey.cligen;

import com.github.asm0dey.cligen.runtime.*;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.github.asm0dey.cligen.runtime.Command"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CliAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "[CLI-GEN] Starting annotation processing round. Annotations: " + annotations
        );


        if (annotations.isEmpty()) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "[CLI-GEN] No annotations to process"
            );
            return false;
        }

        Set<? extends Element> commandElements =
                roundEnv.getElementsAnnotatedWith(Command.class);

        for (Element element : commandElements) {
            try {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.NOTE,
                        "[CLI-GEN] Processing element: " + element.getSimpleName()
                );

                if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.RECORD) {
                    error("@Command can only be applied to classes or records", element);
                    continue;
                }

                TypeElement typeElement = (TypeElement) element;
                processCommand(typeElement);

            } catch (Exception e) {
                error("Error processing @Command: " + e.getMessage(), element);
                e.printStackTrace();
            }
        }

        return true;
    }

    private void processCommand(TypeElement typeElement) throws IOException {
        Command cmdAnnotation = typeElement.getAnnotation(Command.class);
        String commandName = cmdAnnotation.name();
        String qualifiedName = typeElement.getQualifiedName().toString();
        int lastDotIndex = qualifiedName.lastIndexOf('.');
        String packageName = lastDotIndex != -1 ? qualifiedName.substring(0, lastDotIndex) : "";

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "[CLI-GEN] Processing command: " + commandName + " (class: " + qualifiedName + ")"
        );

        // Collect metadata
        Map<String, OptionMetadata> options = new HashMap<>();
        List<ParameterMetadata> parameters = new ArrayList<>();

        List<? extends Element> members = typeElement.getKind() == ElementKind.RECORD
                ? typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.RECORD_COMPONENT)
                .toList()
                : typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .toList();

        for (Element member : members) {
            VariableElement var = (VariableElement) member;
            Option optAnnotation = member.getAnnotation(Option.class);
            Parameters paramAnnotation = member.getAnnotation(Parameters.class);

            if (optAnnotation != null) {
                options.put(var.getSimpleName().toString(),
                        new OptionMetadata(optAnnotation, var));
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.NOTE,
                        "[CLI-GEN] Found option: " + var.getSimpleName() +
                                " with names: " + Arrays.toString(optAnnotation.names())
                );
            }
            if (paramAnnotation != null) {
                parameters.add(new ParameterMetadata(paramAnnotation, var));
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.NOTE,
                        "[CLI-GEN] Found parameter: " + var.getSimpleName() + " at index " + paramAnnotation.index()
                );
            }
        }

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "[CLI-GEN] Command '" + commandName + "' has " + options.size() +
                        " options and " + parameters.size() + " parameters"
        );

        // Generate parser class using JavaPoet
        String parserClassName = capitalizeFirst(typeElement.getSimpleName().toString()) + "CommandParser";
        TypeSpec parserClass = generateParserClass(
                typeElement, commandName, cmdAnnotation, options, parameters
        );

        // Write to source file
        JavaFile javaFile = JavaFile.builder(packageName, parserClass)
                .addFileComment("Generated by CLI-Gen annotation processor")
                .addFileComment("DO NOT EDIT - changes will be overwritten")
                .build();

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "[CLI-GEN] Writing generated parser: " + packageName + "." + parserClassName
        );

        javaFile.writeTo(processingEnv.getFiler());
    }

    private TypeSpec generateParserClass(TypeElement typeElement,
                                         String commandName,
                                         Command cmdAnnotation,
                                         Map<String, OptionMetadata> options,
                                         List<ParameterMetadata> parameters) {

        String simpleClassName = typeElement.getSimpleName().toString();
        String parserClassName = simpleClassName + "CommandParser";

        // Generate parse() method
        MethodSpec parseMethod = generateParseMethod(
                typeElement, simpleClassName, options, parameters, cmdAnnotation
        );

        // Generate getHelpText() method
        MethodSpec helpMethod = generateHelpMethod(commandName, cmdAnnotation, options, parameters);

        // Build the parser class
        return TypeSpec.classBuilder(parserClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(
                        ParameterizedTypeName.get(
                                ClassName.get(CommandParser.class),
                                ClassName.get(typeElement)
                        )
                )
                .addJavadoc("Generated parser for @Command: $L\n", commandName)
                .addJavadoc("Generated at compile time - zero runtime reflection\n")
                .addMethod(parseMethod)
                .addMethod(helpMethod)
                .build();
    }

    private MethodSpec generateParseMethod(TypeElement typeElement,
                                           String commandClassName,
                                           Map<String, OptionMetadata> options,
                                           List<ParameterMetadata> parameters,
                                           Command cmdAnnotation) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "[CLI-GEN] Generating parse method for: " + commandClassName +
                        " with " + options.size() + " options"
        );

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        boolean isRecord = typeElement.getKind().name().equals("RECORD");

        // 1. Initialize variables
        if (isRecord) {
            // Collect all components in order
            List<? extends Element> components = typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind().name().equals("RECORD_COMPONENT"))
                    .collect(java.util.stream.Collectors.toList());

            for (Element component : components) {
                VariableElement var = (VariableElement) component;
                codeBuilder.addStatement("$T $L = $L",
                        TypeName.get(var.asType()),
                        var.getSimpleName().toString(),
                        getDefaultValue(var.asType()));
            }
        } else {
            codeBuilder.addStatement("$L instance = new $L()",
                    commandClassName, commandClassName);
        }

        codeBuilder.addStatement("$T remainingArgs = new $T<>()",
                List.class, ArrayList.class);
        codeBuilder.addStatement("int idx = 0");

        // 2. Parsing loop
        codeBuilder.addStatement("int posIdx = 0");
        codeBuilder.beginControlFlow("while (idx < args.length)");
        codeBuilder.addStatement("$T arg = args[idx]", String.class);

        String target = isRecord ? null : "instance";

        // 3. Generate handlers
        boolean firstArgCondition = true;
        
        // Options
        for (String fieldName : options.keySet()) {
            OptionMetadata meta = options.get(fieldName);
            String[] names = meta.annotation.names();

            CodeBlock.Builder conditionBuilder = CodeBlock.builder();
            for (int i = 0; i < names.length; i++) {
                if (i > 0) conditionBuilder.add(" || ");
                conditionBuilder.add("arg.equals($S)", names[i]);
            }

            if (firstArgCondition) {
                codeBuilder.beginControlFlow("if ($L)", conditionBuilder.build());
                firstArgCondition = false;
            } else {
                codeBuilder.nextControlFlow("else if ($L)", conditionBuilder.build());
            }

            codeBuilder.add(generateOptionHandlerCode(target, fieldName, meta));
            codeBuilder.addStatement("idx++");
        }

        // Positional parameters
        if (firstArgCondition) {
            codeBuilder.beginControlFlow("if (!arg.startsWith($S))", "-");
            firstArgCondition = false;
        } else {
            codeBuilder.nextControlFlow("else if (!arg.startsWith($S))", "-");
        }
        
        if (!parameters.isEmpty()) {
            boolean firstParam = true;
            List<ParameterMetadata> sortedParams = new ArrayList<>(parameters);
            sortedParams.sort(Comparator.comparingInt(p -> p.annotation.index()));

            for (ParameterMetadata param : sortedParams) {
                String fieldName = param.element.getSimpleName().toString();
                int pIdx = param.annotation.index();
                if (firstParam) {
                    codeBuilder.beginControlFlow("if (posIdx == $L)", pIdx);
                    firstParam = false;
                } else {
                    codeBuilder.nextControlFlow("else if (posIdx == $L)", pIdx);
                }
                codeBuilder.addStatement(FieldTypeAnalyzer.getConversionCode(target, fieldName, "arg", param.element.asType()));
            }
            codeBuilder.nextControlFlow("else");
            codeBuilder.addStatement("remainingArgs.add(arg)");
            codeBuilder.endControlFlow();
            codeBuilder.addStatement("posIdx++");
        } else {
            codeBuilder.addStatement("remainingArgs.add(arg)");
        }
        codeBuilder.addStatement("idx++");

        // Unknown options
        codeBuilder.nextControlFlow("else");
        codeBuilder.addStatement("throw new $T($S + arg)",
                ClassName.get(ParseException.class),
                "Unknown option: "
        );
        codeBuilder.endControlFlow(); // end if/else chain

        codeBuilder.endControlFlow(); // end while loop

        // 4. Generate validation for required options and parameters
        for (String fieldName : options.keySet()) {
            OptionMetadata meta = options.get(fieldName);
            if (meta.annotation.required()) {
                VariableElement element = meta.element;
                TypeMirror type = element.asType();

                // Check if null (works for objects)
                if (isObjectType(type)) {
                    codeBuilder.beginControlFlow("if ($L == null)", isRecord ? fieldName : "instance." + fieldName);
                    codeBuilder.addStatement("throw new $T($S)",
                            ClassName.get(ParseException.class),
                            "Required option not provided: " + meta.annotation.names()[0]
                    );
                    codeBuilder.endControlFlow();
                }
            }
        }

        for (ParameterMetadata param : parameters) {
            if (param.annotation.required()) {
                String fieldName = param.element.getSimpleName().toString();
                TypeMirror type = param.element.asType();
                if (isObjectType(type)) {
                    codeBuilder.beginControlFlow("if ($L == null)", isRecord ? fieldName : "instance." + fieldName);
                    codeBuilder.addStatement("throw new $T($S)",
                            ClassName.get(ParseException.class),
                            "Required parameter not provided: " + fieldName
                    );
                    codeBuilder.endControlFlow();
                }
            }
        }

        // 5. Instantiate and return result
        if (isRecord) {
            List<? extends Element> components = typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind().name().equals("RECORD_COMPONENT"))
                    .collect(java.util.stream.Collectors.toList());
            String argsList = components.stream()
                    .map(e -> e.getSimpleName().toString())
                    .collect(java.util.stream.Collectors.joining(", "));
            codeBuilder.addStatement("$L instance = new $L($L)",
                    commandClassName, commandClassName, argsList);
        }

        codeBuilder.addStatement("return new $T<>(instance, remainingArgs)",
                ClassName.get(ParseResult.class)
        );

        return MethodSpec.methodBuilder("parse")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(
                        ParameterizedTypeName.get(
                                ClassName.get(ParseResult.class),
                                TypeName.get(typeElement.asType())
                        )
                )
                .addParameter(String[].class, "args")
                .addException(
                        ClassName.get(ParseException.class)
                )
                .addCode(codeBuilder.build())
                .build();
    }

    private String getDefaultValue(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN: return "false";
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case CHAR: return "0";
            default: return "null";
        }
    }

    private CodeBlock generateOptionHandlerCode(String target, String fieldName, OptionMetadata meta) {
        String arity = meta.annotation.arity();
        boolean takesValue = !arity.equals("0");
        CodeBlock.Builder code = CodeBlock.builder();

        if (takesValue) {
            code.beginControlFlow("if (idx + 1 >= args.length)");
            code.addStatement("throw new $T($S)",
                    ClassName.get(ParseException.class),
                    "Option " + meta.annotation.names()[0] + " requires an argument"
            );
            code.endControlFlow();

            String converterFqn = getConverterFqn(meta);
            if (converterFqn != null) {
                code.beginControlFlow("try");
                code.addStatement("$L$L = (($T<? extends $T>) new $L()).convert(args[idx + 1])",
                        (target == null || target.isEmpty()) ? "" : target + ".",
                        fieldName,
                        ClassName.get("com.github.asm0dey.cligen.runtime", "Converter"),
                        TypeName.get(meta.element.asType()),
                        converterFqn);
                code.nextControlFlow("catch (Exception e)");
                code.addStatement("throw new $T(\"Failed to convert option $L: \" + e.getMessage())",
                        ClassName.get(ParseException.class), meta.annotation.names()[0]);
                code.endControlFlow();
            } else {
                code.addStatement(FieldTypeAnalyzer.getConversionCode(target, fieldName, "args[idx + 1]", meta.element.asType()));
            }
            code.addStatement("idx++");
        } else {
            code.addStatement("$L$L = true", (target == null || target.isEmpty()) ? "" : target + ".", fieldName);
        }

        return code.build();
    }

    private String getConverterFqn(OptionMetadata meta) {
        try {
            Class<?> cls = meta.annotation.converter();
            if (cls == java.lang.Void.class) return null;
            return cls.getCanonicalName();
        } catch (MirroredTypeException mte) {
            String name = mte.getTypeMirror().toString();
            if ("java.lang.Void".equals(name)) return null;
            return name;
        }
    }

    private MethodSpec generateHelpMethod(String commandName,
                                          Command cmdAnnotation,
                                          Map<String, OptionMetadata> options,
                                          List<ParameterMetadata> parameters) {

        // FIX: Build help text properly without escaped newlines
        StringBuilder helpText = new StringBuilder();
        helpText.append(commandName).append(" - ").append(cmdAnnotation.description());
        helpText.append("\n\nUsage: ").append(commandName).append(" [PARAMETERS] [OPTIONS]\n");

        if (!parameters.isEmpty()) {
            helpText.append("\nParameters:\n");
            List<ParameterMetadata> sortedParams = new ArrayList<>(parameters);
            sortedParams.sort(Comparator.comparingInt(p -> p.annotation.index()));
            for (ParameterMetadata param : sortedParams) {
                helpText.append("  ").append(param.element.getSimpleName()).append("\t")
                        .append(param.annotation.description()).append("\n");
            }
        }

        if (!options.isEmpty()) {
            helpText.append("\nOptions:\n");
            for (String fieldName : options.keySet()) {
                OptionMetadata meta = options.get(fieldName);
                String names = String.join(", ", meta.annotation.names());
                helpText.append("  ").append(names).append("\t")
                        .append(meta.annotation.description()).append("\n");
            }
        }

        helpText.append("\n");

        return MethodSpec.methodBuilder("getHelpText")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", helpText.toString())
                .build();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private boolean isObjectType(TypeMirror type) {
        return !type.getKind().isPrimitive();
    }

    private void error(String message, Element element) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                message,
                element
        );
    }

    static class OptionMetadata {
        Option annotation;
        VariableElement element;

        OptionMetadata(Option annotation, VariableElement element) {
            this.annotation = annotation;
            this.element = element;
        }
    }

    static class ParameterMetadata {
        Parameters annotation;
        VariableElement element;

        ParameterMetadata(Parameters annotation, VariableElement element) {
            this.annotation = annotation;
            this.element = element;
        }
    }
}