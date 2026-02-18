import com.github.asm0dey.cligen.runtime.*;

@Command(
    name = "record-webserver",
    description = "Lightweight web server (record version)"
)
public record RecordWebServerConfig(
    @Option(names = {"-p", "--port"}, description = "Server port")
    int port,
    
    @Option(names = {"-h", "--host"}, description = "Bind address")
    String host,
    
    @Option(names = {"-d", "--debug"}, description = "Enable debug logging")
    boolean debug,
    
    @Parameters(index = 0, description = "Document root directory")
    String docRoot
) {
    public void start() {
        System.out.println("Starting RECORD server on " + host + ":" + port);
        System.out.println("Doc root: " + docRoot);
        if (debug) System.out.println("Debug mode ENABLED");
    }
    
    public static void main(String[] args) {
        try {
            RecordWebServerConfigCommandParser parser = new RecordWebServerConfigCommandParser();
            ParseResult<RecordWebServerConfig> result = parser.parse(args);
            result.getCommand().start();
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
