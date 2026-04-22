import java.io.FileInputStream;
import java.util.Properties;

public class ConfigLoader {

    public static Properties load(String path) {
        Properties props = new Properties();

        try {
            FileInputStream fis = new FileInputStream(path);
            props.load(fis);
        } catch (Exception e) {
            System.out.println("Erreur config: " + e.getMessage());
        }

        return props;
    }
}