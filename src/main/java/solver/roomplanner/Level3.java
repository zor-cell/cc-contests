package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level3 {
    private static final Logger log = CustomLogger.getLogger();

    private static final int DESK_LENGTH = 3;
    //side length of the pinwheel block, which leaves only its center cell empty
    private static final int PINWHEEL_SIZE = 5;

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int x = reader.nextInt();
            int y = reader.nextInt();
            int desks = reader.nextInt();

            int[][] room = new int[y][x];
            int placed = placeDesks(room, x, y);

            if(placed != desks) {
                log.severe("Run " + run + ": placed " + placed + " desks in " + x + "x" + y + ", but " + desks + " are required");
            }

            //write result to file
            writer.write(format(room) + "\n");
        }
    }

    /**
     * Places the maximum number of desks in the room and returns how many were placed.
     */
    private static int placeDesks(int[][] room, int x, int y) {
        int id = 0;

        if(x % DESK_LENGTH == 2 && y % DESK_LENGTH == 2 && x >= PINWHEEL_SIZE && y >= PINWHEEL_SIZE) {
            //rows-then-strip would waste a 2x2 corner here, the pinwheel wastes a single cell
            id = placePinwheel(room, id);
            id = fillHorizontal(room, PINWHEEL_SIZE, 0, x, PINWHEEL_SIZE, id);
            id = fillVertical(room, 0, PINWHEEL_SIZE, x, y, id);
        } else {
            //horizontal desks first, the leftover strip on the right (width 1 or 2) gets vertical desks
            int stripStart = x - x % DESK_LENGTH;
            id = fillHorizontal(room, 0, 0, stripStart, y, id);
            id = fillVertical(room, stripStart, 0, x, y, id);
        }

        return id;
    }

    /**
     * Places 8 desks in the top left 5x5 block, leaving only the center cell empty.
     */
    private static int placePinwheel(int[][] room, int id) {
        id = fillHorizontal(room, 0, 0, 3, 2, id);
        id = fillVertical(room, 3, 0, 5, 3, id);
        id = fillHorizontal(room, 2, 3, 5, 5, id);
        id = fillVertical(room, 0, 2, 2, 5, id);

        return id;
    }

    /**
     * Fills the area [fromX, toX) x [fromY, toY) with horizontal desks, as many per row as fit.
     */
    private static int fillHorizontal(int[][] room, int fromX, int fromY, int toX, int toY, int id) {
        for(int row = fromY;row < toY;row++) {
            for(int col = fromX;col + DESK_LENGTH <= toX;col += DESK_LENGTH) {
                id++;
                for(int i = 0;i < DESK_LENGTH;i++) {
                    room[row][col + i] = id;
                }
            }
        }

        return id;
    }

    /**
     * Fills the area [fromX, toX) x [fromY, toY) with vertical desks, as many per column as fit.
     */
    private static int fillVertical(int[][] room, int fromX, int fromY, int toX, int toY, int id) {
        for(int col = fromX;col < toX;col++) {
            for(int row = fromY;row + DESK_LENGTH <= toY;row += DESK_LENGTH) {
                id++;
                for(int i = 0;i < DESK_LENGTH;i++) {
                    room[row + i][col] = id;
                }
            }
        }

        return id;
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
