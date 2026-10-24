package solver.lawnmower;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level1 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            String path = reader.nextLine().trim();

            //count occurrences of each direction
            int w = 0, d = 0, s = 0, a = 0;
            for(char c : path.toCharArray()) {
                switch(c) {
                    case 'W' -> w++;
                    case 'D' -> d++;
                    case 'S' -> s++;
                    case 'A' -> a++;
                    default -> throw new IllegalArgumentException("Unknown direction '" + c + "' in run " + run);
                }
            }

            //write result to file
            writer.write(w + " " + d + " " + s + " " + a + "\n");
        }
    }
}
