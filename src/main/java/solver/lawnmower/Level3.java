package solver.lawnmower;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level3 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
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

            String path = reader.nextLine().trim();

            //write result to file
            writer.write((isValid(blocked, width, height, path) ? "VALID" : "INVALID") + "\n");
        }
    }

    /**
     * Checks whether {@code path} visits every free cell exactly once without touching a blocked cell or leaving the lawn.
     * The start cell is not given: a path covering the whole lawn has to span exactly the lawn's bounding box, which fixes the start.
     */
    public static boolean isValid(boolean[][] blocked, int width, int height, String path) {
        int x = 0, y = 0;
        int minX = 0, maxX = 0, minY = 0, maxY = 0;
        for(char c : path.toCharArray()) {
            switch(c) {
                case 'W' -> y--;
                case 'D' -> x++;
                case 'S' -> y++;
                case 'A' -> x--;
                default -> throw new IllegalArgumentException("Unknown direction '" + c + "'");
            }

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        //smaller box cannot cover the lawn, bigger box leaves it
        if(maxX - minX + 1 != width || maxY - minY + 1 != height) return false;

        int freeCells = 0;
        for(int r = 0;r < height;r++) {
            for(int c = 0;c < width;c++) {
                if(!blocked[r][c]) freeCells++;
            }
        }

        boolean[][] visited = new boolean[height][width];
        x = -minX;
        y = -minY;
        if(blocked[y][x]) return false;
        visited[y][x] = true;
        int visitedCells = 1;

        for(char c : path.toCharArray()) {
            switch(c) {
                case 'W' -> y--;
                case 'D' -> x++;
                case 'S' -> y++;
                case 'A' -> x--;
            }

            if(blocked[y][x] || visited[y][x]) return false;
            visited[y][x] = true;
            visitedCells++;
        }

        return visitedCells == freeCells;
    }
}
