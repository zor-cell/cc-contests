package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * A desk of length L plus the free cells it needs to its right and below covers a 2 by (L + 1) block. Two desks touch
 * exactly when their blocks overlap, so placing non-touching desks in an x by y room is the same as packing blocks
 * into an (x + 1) by (y + 1) area (the extra row and column account for desks that touch the right and bottom wall).
 * Here the block length may differ per desk and the number of covered cells counts, not the number of desks.
 */
public class Level7 {
    private static final Logger log = CustomLogger.getLogger();

    private static final DeskPacking packing = new DeskPacking();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int x = reader.nextInt();
            int y = reader.nextInt();
            int cells = reader.nextInt();

            boolean[][] room = new boolean[y][x];
            int covered = placeDesks(room, x, y);

            if(covered != cells) {
                log.severe("Run " + run + ": covered " + covered + " cells in " + x + "x" + y + ", but " + cells + " are required");
            }

            //write result to file
            writer.write(format(room) + "\n");
        }
    }

    /**
     * Covers as many cells as possible with non-touching desks and returns how many were covered.
     */
    private static int placeDesks(boolean[][] room, int x, int y) {
        List<int[]> desks = new ArrayList<>();
        packing.place(x + 1, y + 1, 0, 0, desks);

        int covered = 0;
        for(int[] desk : desks) {
            boolean lying = desk[2] == 1;
            for(int i = 0;i < desk[3];i++) {
                room[desk[0] + (lying ? 0 : i)][desk[1] + (lying ? i : 0)] = true;
            }
            covered += desk[3];
        }

        return covered;
    }

    private static String format(boolean[][] room) {
        StringBuilder sb = new StringBuilder();
        for(boolean[] row : room) {
            for(boolean desk : row) {
                sb.append(desk ? 'X' : '.');
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    /**
     * Packs desk blocks into rectangles so that the desks cover as many cells as possible. A rectangle is either
     * filled with lines of desks of one orientation, cut in two, or split like a pinwheel into four rectangles around
     * a fifth one; the parts are packed the same way. The best split is memoized per rectangle size.
     */
    private static class DeskPacking {
        private static final int MAX_DESK_LENGTH = 7;
        private static final int BLOCK_SHORT = 2;
        private static final int MAX_BLOCK_LONG = MAX_DESK_LENGTH + 1;
        //the pinwheel split is expensive and only pays off around small leftovers
        private static final int PINWHEEL_MAX_SIDE = 30;

        private static final int LYING = 1;
        private static final int UPRIGHT = 2;
        private static final int CUT_VERTICAL = 3;
        private static final int CUT_HORIZONTAL = 4;
        private static final int PINWHEEL = 5;

        //both indexed by [width][height]; a value of -1 means not calculated yet
        private int[][] cells = new int[0][0];
        private int[][][] plan = new int[0][0][];

        /**
         * Adds the desks for the rectangle with the given size and top left corner as
         * {row, col, lying ? 1 : 0, length}.
         */
        void place(int width, int height, int row, int col, List<int[]> desks) {
            if(best(width, height) == 0) return;

            int[] p = plan[width][height];
            switch(p[0]) {
                case LYING -> {
                    for(int r = 0;r + BLOCK_SHORT <= height;r += BLOCK_SHORT) {
                        placeLine(width, row + r, col, true, desks);
                    }
                }
                case UPRIGHT -> {
                    for(int c = 0;c + BLOCK_SHORT <= width;c += BLOCK_SHORT) {
                        placeLine(height, row, col + c, false, desks);
                    }
                }
                case CUT_VERTICAL -> {
                    place(p[1], height, row, col, desks);
                    place(width - p[1], height, row, col + p[1], desks);
                }
                case CUT_HORIZONTAL -> {
                    place(width, p[1], row, col, desks);
                    place(width, height - p[1], row + p[1], col, desks);
                }
                case PINWHEEL -> {
                    int x1 = p[1], x2 = p[2], y1 = p[3], y2 = p[4];
                    place(x2, y1, row, col, desks);
                    place(width - x2, y2, row, col + x2, desks);
                    place(width - x1, height - y2, row + y2, col + x1, desks);
                    place(x1, height - y1, row + y1, col, desks);
                    place(x2 - x1, y2 - y1, row + y1, col + x1, desks);
                }
                default -> throw new IllegalStateException("Unknown plan " + p[0]);
            }
        }

        /**
         * Adds one line of desks, as long as possible each, to a strip of the given length.
         */
        private void placeLine(int length, int row, int col, boolean lying, List<int[]> desks) {
            for(int offset = 0;offset + BLOCK_SHORT <= length;offset += MAX_BLOCK_LONG) {
                int deskLength = Math.min(MAX_BLOCK_LONG, length - offset) - 1;
                desks.add(new int[]{row + (lying ? 0 : offset), col + (lying ? offset : 0), lying ? 1 : 0, deskLength});
            }
        }

        /**
         * The number of cells one line of desks covers in a strip of the given length: every desk costs one free cell.
         */
        private int lineCells(int length) {
            return length - (length + MAX_BLOCK_LONG - 1) / MAX_BLOCK_LONG;
        }

        /**
         * The highest number of covered cells found for a rectangle of the given size.
         */
        private int best(int width, int height) {
            if(width < BLOCK_SHORT || height < BLOCK_SHORT) return 0;

            ensureCapacity(Math.max(width, height));
            if(cells[width][height] >= 0) return cells[width][height];

            int lying = (height / BLOCK_SHORT) * lineCells(width);
            int upright = (width / BLOCK_SHORT) * lineCells(height);

            int max = lying;
            int[] p = {LYING};
            if(upright > max) {
                max = upright;
                p = new int[]{UPRIGHT};
            }

            for(int cut = 1;cut <= width / 2;cut++) {
                int value = best(cut, height) + best(width - cut, height);
                if(value > max) {
                    max = value;
                    p = new int[]{CUT_VERTICAL, cut};
                }
            }

            for(int cut = 1;cut <= height / 2;cut++) {
                int value = best(width, cut) + best(width, height - cut);
                if(value > max) {
                    max = value;
                    p = new int[]{CUT_HORIZONTAL, cut};
                }
            }

            if(width <= PINWHEEL_MAX_SIDE && height <= PINWHEEL_MAX_SIDE) {
                for(int x1 = 1;x1 < width - 1;x1++) {
                    for(int x2 = x1 + 1;x2 < width;x2++) {
                        for(int y1 = 1;y1 < height - 1;y1++) {
                            for(int y2 = y1 + 1;y2 < height;y2++) {
                                int value = best(x2, y1) + best(width - x2, y2) + best(width - x1, height - y2)
                                        + best(x1, height - y1) + best(x2 - x1, y2 - y1);
                                if(value > max) {
                                    max = value;
                                    p = new int[]{PINWHEEL, x1, x2, y1, y2};
                                }
                            }
                        }
                    }
                }
            }

            cells[width][height] = max;
            plan[width][height] = p;

            return max;
        }

        /**
         * Grows the tables so that they can hold rectangles up to the given side length. Known results are kept.
         */
        private void ensureCapacity(int side) {
            if(side < cells.length) return;

            int[][] newCells = new int[side + 1][side + 1];
            int[][][] newPlan = new int[side + 1][side + 1][];
            for(int[] column : newCells) {
                Arrays.fill(column, -1);
            }

            for(int width = 0;width < cells.length;width++) {
                System.arraycopy(cells[width], 0, newCells[width], 0, cells[width].length);
                System.arraycopy(plan[width], 0, newPlan[width], 0, plan[width].length);
            }

            cells = newCells;
            plan = newPlan;
        }
    }
}
