package logreader.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Created by karltrout on 5/15/17.
 */
public class testing {

    private static final Path o = Paths.get("/Users/karltrout/Documents/Resources/text/HADDS/");
    private static Path r = Paths.get("/Users/karltrout/Documents/Resources/hadds/");

    public static void main(String[] args) {

        List<Path> files = new ArrayList<>();
        int maxDepth = 10;

        String pattern = "nas_[0-9]{2}\\.log";
        Number totalBytesCollected = 0;
        Number proccessedBytes = 0;

        try (Stream<Path> stream = Files.walk(r, maxDepth)) {
            files = stream
                    .filter(path -> path.getFileName().toString().matches(pattern))
                    .collect(toList());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        for ( Path p : files )
        {
            String outFileName = p.getFileName().toString().replace(".log", "_flat.txt");
            Path f = o.resolve(r.relativize(p)).getParent().resolve(outFileName);
            System.out.println(f);


        }
    }

}
