package solver.spaceship;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level5 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine();

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            String line = reader.nextLine();
            String[] split = line.split(" ");
            Point s = new Point(split[0]);
            int limit = Integer.parseInt(split[1]);

            String aLine = reader.nextLine();
            Point a = new Point(aLine);

            var path = PathFinder.findPath(s, limit, List.of(a));
            if(path == null) {
                log.severe("Run " + run + ": no path within the time limit");
                path = new TotalPath(List.of(0), List.of(0));
            }

            printPath(path.x, writer);
            printPath(path.y, writer);
            writer.write("\n");
        }
    }

    private static void printPath(List<Integer> path, FileWriter writer) throws IOException {
        for(var a : path) {
            writer.write(a + " ");
        }
        writer.write("\n");
    }
}
