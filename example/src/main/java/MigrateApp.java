import com.github.asm0dey.cligen.runtime.Command;
import com.github.asm0dey.cligen.runtime.Option;
import com.github.asm0dey.cligen.runtime.Parameters;
import com.github.asm0dey.cligen.runtime.ParseException;

@Command(
    name = "migrate",
    description = "Database migration utility"
)
public class MigrateApp {
    
    @Option(names = {"-H", "--host"},
            required = true,
            description = "Database host")
    public String dbHost;
    
    @Option(names = {"-U", "--user"}, 
            required = true,
            description = "Database user")
    public String dbUser;
    
    @Option(names = {"-P", "--password"}, 
            description = "Database password")
    public String dbPassword;
    
    @Option(names = {"--port"}, 
            description = "Database port (default: 5432)")
    public int dbPort = 5432;
    
    @Option(names = {"--dry-run"}, 
            description = "Show what would be executed without executing")
    public boolean dryRun = false;
    
    @Parameters(index = 0,
                description = "Migration command (up, down, status)")
    public String command;
    
    @Parameters(index = 1,
                required = false,
                description = "Target version (optional)")
    public String targetVersion;
    
    public void execute() {
        System.out.println("Connecting to " + dbHost + ":" + dbPort);
        System.out.println("User: " + dbUser);
        
        if (dryRun) {
            System.out.println("[DRY RUN] Command: " + command);
        } else {
            System.out.println("Executing: " + command);
        }
        
        if (targetVersion != null) {
            System.out.println("Target version: " + targetVersion);
        }
    }
    
    public static void main(String[] args) {
        try {
            MigrateAppCommandParser parser = new MigrateAppCommandParser();
            MigrateApp app = parser.parse(args).getCommand();
            app.execute();
        } catch (ParseException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}
