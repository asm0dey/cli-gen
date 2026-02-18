# CLI-Gen

A lightweight, annotation-based CLI parser generator for Java. `cli-gen` uses annotation processing to generate type-safe command-line parsers at compile time, reducing runtime overhead and providing a clean API.

## Features

- **Compile-time generation**: No reflection at runtime.
- **Type-safe**: Generated parsers return your specific command classes.
- **Java Records support**: Works seamlessly with Java 14+ records.
- **Multi-command support**: Built-in `CommandDispatcher` for applications with multiple subcommands.
- **Automatic help generation**: Generates help and version information based on annotations.
- **Customizable**: Support for required options, default values, and custom converters.

## Installation

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.github.asm0dey.cligen</groupId>
        <artifactId>cli-runtime</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.github.asm0dey.cligen</groupId>
                        <artifactId>cli-processor</artifactId>
                        <version>1.0-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Usage

### Single Command

Define your configuration class and annotate it with `@Command`, `@Option`, and `@Parameters`.

```java
import com.github.asm0dey.cligen.runtime.*;

@Command(
    name = "webserver",
    description = "Lightweight web server",
    version = "1.0.0",
    mixinStandardHelpOptions = true
)
public class WebServerConfig {
    
    @Option(names = {"-p", "--port"}, description = "Server port", required = false)
    public int port = 8080;
    
    @Option(names = {"-h", "--host"}, description = "Bind address")
    public String host = "0.0.0.0";
    
    @Parameters(index = 0, description = "Document root directory")
    public String docRoot;

    public void start() {
        System.out.println("Starting server on " + host + ":" + port);
    }
}
```

The annotation processor will generate a `WebServerConfigCommandParser` class. You can use it in your `main` method:

```java
public static void main(String[] args) {
    try {
        WebServerConfigCommandParser parser = new WebServerConfigCommandParser();
        ParseResult<WebServerConfig> result = parser.parse(args);
        result.getCommand().start();
    } catch (ParseException e) {
        System.err.println("Error: " + e.getMessage());
        System.exit(1);
    }
}
```

### Multi-command Support with `CommandDispatcher`

For applications with multiple subcommands (like `git` or `docker`), use the `CommandDispatcher`.

1. Define your command classes:

```java
@Command(name = "server", description = "Run the server")
public record ServerCmd(...) { ... }

@Command(name = "migrate", description = "Run migrations")
public class MigrateCmd { ... }
```

2. Set up the `CommandDispatcher`:

```java
import com.github.asm0dey.cligen.runtime.*;

public class App {
    public static void main(String[] args) {
        CommandDispatcher dispatcher = new CommandDispatcher("myapp", "1.0.0");
        
        // Register generated parsers
        dispatcher.register("server", new ServerCmdCommandParser());
        dispatcher.register("migrate", new MigrateCmdCommandParser());
        
        try {
            ParseResult<?> result = dispatcher.dispatch(args);
            if (result == null) return; // Help or version was shown
            
            Object cmd = result.getCommand();
            if (cmd instanceof ServerCmd s) s.run();
            else if (cmd instanceof MigrateCmd m) m.execute();
            
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
```

### Java Records Support

`cli-gen` fully supports Java Records. Options and parameters can be defined directly in the record components:

```java
@Command(name = "record-app", description = "Demo with records")
public record RecordApp(
    @Option(names = "-v") boolean verbose,
    @Parameters(index = 0) String input
) {}
```

## Help and Version

- Use `--help` or `-h` to see automatically generated help text.
- Use `--version` or `-v` to see the application version (if configured in `@Command` or `CommandDispatcher`).
