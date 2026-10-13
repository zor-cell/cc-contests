package solver.lawnmower;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level2 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            String path = reader.nextLine().trim();

            //track bounding box of all visited cells, starting cell is (0, 0)
            int x = 0, y = 0;
            int minX = 0, maxX = 0, minY = 0, maxY = 0;
            for(char c : path.toCharArray()) {
                switch(c) {
                    case 'W' -> y--;
                    case 'D' -> x++;
                    case 'S' -> y++;
                    case 'A' -> x--;
                    default -> throw new IllegalArgumentException("Unknown direction '" + c + "' in run " + run);
                }

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;

            //write result to file
            writer.write(width + " " + height + "\n");
        }
    }
}
