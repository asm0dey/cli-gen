import com.github.asm0dey.cligen.runtime.*;

@Command(
    name = "webserver",
    description = "Lightweight web server",
    version = "1.0.0",
    mixinStandardHelpOptions = true
)
public class WebServerConfig {
    
    @Option(names = {"-p", "--port"}, 
            description = "Server port (default: 8080)")
    public int port = 8080;
    
    @Option(names = {"-h", "--host"}, 
            description = "Bind address")
    public String host = "0.0.0.0";
    
    @Option(names = {"-t", "--threads"},
            description = "Worker threads")
    public int threads = Runtime.getRuntime().availableProcessors();
    
    @Option(names = {"-d", "--debug"}, 
            description = "Enable debug logging")
    public boolean debug = false;
    
    @Parameters(index = 0,
                description = "Document root directory")
    public String docRoot;
    
    public void start() {
        System.out.println("Starting server on " + host + ":" + port);
        System.out.println("Threads: " + threads);
        System.out.println("Doc root: " + docRoot);
        if (debug) System.out.println("Debug mode ENABLED");
    }
    
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
}
