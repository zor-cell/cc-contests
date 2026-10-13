package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level2 {
    private static final Logger log = CustomLogger.getLogger();

    private static final int DESK_LENGTH = 3;

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int x = reader.nextInt();
            int y = reader.nextInt();
            int desks = reader.nextInt();

            int[][] room = placeDesks(x, y);

            int placed = room[y - 1][x - 1];
            if(placed != desks) {
                log.severe("Run " + run + ": placed " + placed + " desks, but " + desks + " are required");
            }

            //write result to file
            writer.write(format(room) + "\n");
        }
    }

    /**
     * Fills the room row by row with horizontal desks. Since x is divisible by the desk length, no cell stays empty.
     */
    private static int[][] placeDesks(int x, int y) {
        int[][] room = new int[y][x];

        int id = 0;
        for(int row = 0;row < y;row++) {
            for(int col = 0;col < x;col++) {
                if(col % DESK_LENGTH == 0) id++;

                room[row][col] = id;
            }
        }

        return room;
    }

    private static String format(int[][] room) {
        StringBuilder sb = new StringBuilder();
        for(int[] row : room) {
            for(int col = 0;col < row.length;col++) {
                if(col > 0) sb.append(' ');
                sb.append(row[col]);
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}
