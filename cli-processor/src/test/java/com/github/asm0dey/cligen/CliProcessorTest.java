package com.github.asm0dey.cligen;

import com.google.common.truth.StringSubject;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;

public class CliProcessorTest {

    @Test
    public void testGeneratesParserForSimpleCommand() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.MyCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"my\", description = \"Simple command\")\n"
                        + "public class MyCmd {\n"
                        + "    @Option(names = {\"-v\", \"--verbose\"})\n"
                        + "    public boolean verbose;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.MyCmdCommandParser")
                .isNotNull();
    }

    @Test
    public void testGeneratesParserWithRequiredOption() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.RequiredCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"required\")\n"
                        + "public class RequiredCmd {\n"
                        + "    @Option(names = {\"-f\", \"--file\"}, required = true)\n"
                        + "    public String filename;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.RequiredCmdCommandParser")
                .isNotNull();
    }

    @Test
    public void testValidatesAnnotationOnlyOnClasses() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.InvalidUse",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"invalid\")\n"
                        + "public interface InvalidUse { }\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation)
                .hadErrorContaining("@Command can only be applied to classes");
    }

    @Test
    public void testGeneratesValidParseMethod() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.GitCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"git\", description = \"Version control\")\n"
                        + "public class GitCmd {\n"
                        + "    @Option(names = {\"-v\", \"--verbose\"})\n"
                        + "    public boolean verbose;\n"
                        + "    @Option(names = {\"-C\"}, required = true)\n"
                        + "    public String directory;\n"
                        + "    @Parameters(index = 0)\n"
                        + "    public String command;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();

        // Verify the generated file contains key methods
        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.GitCmdCommandParser")
                .contentsAsUtf8String()
                .contains("public ParseResult<GitCmd> parse(String[] args)");

        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.GitCmdCommandParser")
                .contentsAsUtf8String()
                .contains("@Override");

        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.GitCmdCommandParser")
                .contentsAsUtf8String()
                .contains("CommandParser<GitCmd>");
    }

    @Test
    public void testGeneratedCodeImplementsInterface() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.TestCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"test\")\n"
                        + "public class TestCmd {\n"
                        + "    @Option(names = {\"-x\"})\n"
                        + "    public String option;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();

        StringSubject generated = assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.TestCmdCommandParser")
                .contentsAsUtf8String();

        // Verify it implements CommandParser interface
        generated.contains("implements CommandParser<TestCmd>");

        // Verify parse method exists
        generated.contains("public ParseResult<TestCmd> parse");

        // Verify getHelpText method exists
        generated.contains("public String getHelpText()");
    }

    @Test
    public void testHandlesMultipleOptions() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.MultiOptCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"multi\")\n"
                        + "public class MultiOptCmd {\n"
                        + "    @Option(names = {\"-a\", \"--alpha\"})\n"
                        + "    public boolean alpha;\n"
                        + "    @Option(names = {\"-b\", \"--beta\"})\n"
                        + "    public String beta;\n"
                        + "    @Option(names = {\"-c\", \"--gamma\"})\n"
                        + "    public int gamma;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
    }

    @Test
    public void testImportsAreGenerated() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.ImportsCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"imports\")\n"
                        + "public class ImportsCmd {\n"
                        + "    @Option(names = {\"-x\"})\n"
                        + "    public String x;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();

        StringSubject generated = assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.ImportsCmdCommandParser")
                .contentsAsUtf8String();

        // Verify required imports
        generated
                .contains("import com.github.asm0dey.cligen.runtime.CommandParser");
        generated
                .contains("import com.github.asm0dey.cligen.runtime.ParseResult");
        generated
                .contains("import com.github.asm0dey.cligen.runtime.ParseException");
        generated
                .contains("import java.util.ArrayList");
        generated
                .contains("import java.util.List");
    }

    @Test
    public void testGeneratesParserInSamePackage() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "org.pkg.deep.DeepPkgCmd",
                "package org.pkg.deep;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"deep\")\n"
                        + "public class DeepPkgCmd {\n"
                        + "    @Option(names = {\"-d\"})\n"
                        + "    public String data;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("org.pkg.deep.DeepPkgCmdCommandParser")
                .isNotNull();
    }

    @Test
    public void testCustomConverterSupport() {
        JavaFileObject converter = JavaFileObjects.forSourceString(
                "com.github.asm0dey.DateConverter",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.Converter;\n"
                        + "import java.text.SimpleDateFormat;\n"
                        + "import java.util.Date;\n"
                        + "public class DateConverter implements Converter<Date> {\n"
                        + "    @Override\n"
                        + "    public Date convert(String s) throws Exception {\n"
                        + "        return new SimpleDateFormat(\"yyyy-MM-dd\").parse(s);\n"
                        + "    }\n"
                        + "}\n"
        );

        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.DateCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "import java.util.Date;\n"
                        + "@Command(name = \"date\")\n"
                        + "public class DateCmd {\n"
                        + "    @Option(names = {\"-d\"}, arity=\"1\", description=\"date option\", converter=DateConverter.class)\n"
                        + "    public Date date;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source, converter);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.DateCmdCommandParser")
                .contentsAsUtf8String()
                .contains("((Converter<? extends Date>) new com.github.asm0dey.DateConverter()).convert");
    }

    @Test
    public void testPositionalParametersInHelp() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.PosCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"pos\", description = \"Positional test\")\n"
                        + "public class PosCmd {\n"
                        + "    @Parameters(index = 0, description = \"First parameter\")\n"
                        + "    public String first;\n"
                        + "    @Parameters(index = 1, description = \"Second parameter\")\n"
                        + "    public String second;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();

        StringSubject generated = assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.PosCmdCommandParser")
                .contentsAsUtf8String();

        generated.contains("Usage: pos [PARAMETERS] [OPTIONS]");
        generated.contains("Parameters:");
        generated.contains("first");
        generated.contains("First parameter");
        generated.contains("second");
        generated.contains("Second parameter");
        
        // Check for multiple parameters handling in parse method
        generated.contains("instance.first = arg");
        generated.contains("instance.second = arg");
    }

    @Test
    public void testOptionalPositionalParameters() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.OptPosCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"optpos\")\n"
                        + "public class OptPosCmd {\n"
                        + "    @Parameters(index = 0, required = true)\n"
                        + "    public String required;\n"
                        + "    @Parameters(index = 1, required = false)\n"
                        + "    public String optional;\n"
                        + "}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();

        StringSubject generated = assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.OptPosCmdCommandParser")
                .contentsAsUtf8String();

        generated.contains("if (instance.required == null)");
        generated.doesNotContain("if (instance.optional == null)");
    }

    @Test
    public void testGeneratesParserForRecordCommand() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "com.github.asm0dey.RecordCmd",
                "package com.github.asm0dey;\n"
                        + "import com.github.asm0dey.cligen.runtime.*;\n"
                        + "\n"
                        + "@Command(name = \"record-cmd\", description = \"Record command\")\n"
                        + "public record RecordCmd(\n"
                        + "    @Option(names = {\"-v\", \"--verbose\"}) boolean verbose,\n"
                        + "    @Option(names = {\"-n\", \"--name\"}) String name,\n"
                        + "    @Parameters(index = 0) String firstParam\n"
                        + ") {}\n"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new CliAnnotationProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.RecordCmdCommandParser")
                .isNotNull();
        
        StringSubject generated = assertThat(compilation)
                .generatedSourceFile("com.github.asm0dey.RecordCmdCommandParser")
                .contentsAsUtf8String();
        
        generated.contains("new RecordCmd(verbose, name, firstParam)");
        generated.contains("firstParam = arg");
    }
}