package solver.lawnmower;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level4 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        solve(reader, writer, false);
    }

    /**
     * Writes a valid path for each lawn. If {@code closed} is set, the path has to end next to its start cell.
     */
    public static void solve(Scanner reader, FileWriter writer, boolean closed) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int width = reader.nextInt();
            int height = reader.nextInt();
            reader.nextLine(); //skip linebreak

            boolean[][] blocked = new boolean[height][width];
            for(int r = 0;r < height;r++) {
                String line = reader.nextLine();
                for(int c = 0;c < width;c++) {
                    blocked[r][c] = line.charAt(c) != '.';
                }
            }

            long start = System.currentTimeMillis();
            String path = new PathFinder(blocked, width, height).find(run, 50, closed);
            log.fine("Run " + run + " (" + width + "x" + height + ") took " + (System.currentTimeMillis() - start) + "ms");

            //check with the validator of the previous level
            if(path == null || !Level3.isValid(blocked, width, height, path) || (closed && !endsNextToStart(path))) {
                log.severe("No valid path found for run " + run + " (" + width + "x" + height + ")");
                path = "";
            }

            //write result to file
            writer.write(path + "\n");
        }
    }

    private static boolean endsNextToStart(String path) {
        int x = 0, y = 0;
        for(char c : path.toCharArray()) {
            switch(c) {
                case 'W' -> y--;
                case 'D' -> x++;
                case 'S' -> y++;
                case 'A' -> x--;
            }
        }

        return Math.abs(x) + Math.abs(y) == 1;
    }
}
