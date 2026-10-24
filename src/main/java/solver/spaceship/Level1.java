package solver.spaceship;

import io.IOUtils;
import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level1 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            //compute sum of each line
            List<Integer> list = IOUtils.readList(reader, " ", Integer::parseInt);
            int sum = list.stream().mapToInt(Integer::intValue).sum();

            //write result to file
            writer.write(sum + "\n");
        }
    }
}
