package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * A desk of length L plus the free cells it needs to its right and below covers a 2 by (L + 1) block. Two desks touch
 * exactly when their blocks overlap, so placing non-touching desks in an x by y room is the same as packing blocks
 * into an (x + 1) by (y + 1) area (the extra row and column account for desks that touch the right and bottom wall).
 */
public class Level6 {
    private static final Logger log = CustomLogger.getLogger();

    //desk length -> packing table for the blocks of that desk
    private static final Map<Integer, BlockPacking> packings = new HashMap<>();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int x = reader.nextInt();
            int y = reader.nextInt();
            int desks = reader.nextInt();
            int deskLength = reader.nextInt();

            boolean[][] room = new boolean[y][x];
            int placed = placeDesks(room, x, y, deskLength);

            if(placed != desks) {
                log.severe("Run " + run + ": placed " + placed + " desks of length " + deskLength + " in " + x + "x" + y + ", but " + desks + " are required");
            }

            //write result to file
            writer.write(format(room) + "\n");
        }
    }

    /**
     * Places as many non-touching desks as possible in the room and returns how many were placed.
     */
    private static int placeDesks(boolean[][] room, int x, int y, int deskLength) {
        BlockPacking packing = packings.computeIfAbsent(deskLength, k -> new BlockPacking(deskLength + 1));

        List<int[]> blocks = new ArrayList<>();
        packing.place(x + 1, y + 1, 0, 0, blocks);

        for(int[] block : blocks) {
            boolean lying = block[2] == 1;
            for(int i = 0;i < deskLength;i++) {
                room[block[0] + (lying ? 0 : i)][block[1] + (lying ? i : 0)] = true;
            }
        }

        return blocks.size();
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
     * Packs blocks of size 2 by blockLong into rectangles. A rectangle is either filled with blocks of one
     * orientation, cut in two, or split like a pinwheel into four rectangles around a fifth one; the parts are packed
     * the same way. The best split is memoized per rectangle size.
     */
    private static class BlockPacking {
        private static final int BLOCK_SHORT = 2;
        //the pinwheel split is expensive and only pays off around small leftovers
        private static final int PINWHEEL_MAX_SIDE = 30;

        private static final int LYING = 1;
        private static final int UPRIGHT = 2;
        private static final int CUT_VERTICAL = 3;
        private static final int CUT_HORIZONTAL = 4;
        private static final int PINWHEEL = 5;

        private final int blockLong;

        //both indexed by [width][height]; a count of -1 means not calculated yet
        private int[][] count = new int[0][0];
        private int[][][] plan = new int[0][0][];

        BlockPacking(int blockLong) {
            this.blockLong = blockLong;
        }

        /**
         * Adds the blocks for the rectangle with the given size and top left corner as {row, col, lying ? 1 : 0}.
         */
        void place(int width, int height, int row, int col, List<int[]> blocks) {
            if(best(width, height) == 0) return;

            int[] p = plan[width][height];
            switch(p[0]) {
                case LYING -> {
                    for(int r = 0;r + BLOCK_SHORT <= height;r += BLOCK_SHORT) {
                        for(int c = 0;c + blockLong <= width;c += blockLong) {
                            blocks.add(new int[]{row + r, col + c, 1});
                        }
                    }
                }
                case UPRIGHT -> {
                    for(int r = 0;r + blockLong <= height;r += blockLong) {
                        for(int c = 0;c + BLOCK_SHORT <= width;c += BLOCK_SHORT) {
                            blocks.add(new int[]{row + r, col + c, 0});
                        }
                    }
                }
                case CUT_VERTICAL -> {
                    place(p[1], height, row, col, blocks);
                    place(width - p[1], height, row, col + p[1], blocks);
                }
                case CUT_HORIZONTAL -> {
                    place(width, p[1], row, col, blocks);
                    place(width, height - p[1], row + p[1], col, blocks);
                }
                case PINWHEEL -> {
                    int x1 = p[1], x2 = p[2], y1 = p[3], y2 = p[4];
                    place(x2, y1, row, col, blocks);
                    place(width - x2, y2, row, col + x2, blocks);
                    place(width - x1, height - y2, row + y2, col + x1, blocks);
                    place(x1, height - y1, row + y1, col, blocks);
                    place(x2 - x1, y2 - y1, row + y1, col + x1, blocks);
                }
                default -> throw new IllegalStateException("Unknown plan " + p[0]);
            }
        }

        /**
         * The highest number of blocks found for a rectangle of the given size.
         */
        private int best(int width, int height) {
            if(width < BLOCK_SHORT || height < BLOCK_SHORT) return 0;

            ensureCapacity(Math.max(width, height));
            if(count[width][height] >= 0) return count[width][height];

            //more blocks than this cannot fit by area
            int bound = width * height / (BLOCK_SHORT * blockLong);

            int lying = (width / blockLong) * (height / BLOCK_SHORT);
            int upright = (width / BLOCK_SHORT) * (height / blockLong);

            int max = lying;
            int[] p = {LYING};
            if(upright > max) {
                max = upright;
                p = new int[]{UPRIGHT};
            }

            for(int cut = 1;cut <= width / 2 && max < bound;cut++) {
                int value = best(cut, height) + best(width - cut, height);
                if(value > max) {
                    max = value;
                    p = new int[]{CUT_VERTICAL, cut};
                }
            }

            for(int cut = 1;cut <= height / 2 && max < bound;cut++) {
                int value = best(width, cut) + best(width, height - cut);
                if(value > max) {
                    max = value;
                    p = new int[]{CUT_HORIZONTAL, cut};
                }
            }

            if(width <= PINWHEEL_MAX_SIDE && height <= PINWHEEL_MAX_SIDE) {
                for(int x1 = 1;x1 < width - 1 && max < bound;x1++) {
                    for(int x2 = x1 + 1;x2 < width && max < bound;x2++) {
                        for(int y1 = 1;y1 < height - 1 && max < bound;y1++) {
                            for(int y2 = y1 + 1;y2 < height && max < bound;y2++) {
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

            count[width][height] = max;
            plan[width][height] = p;

            return max;
        }

        /**
         * Grows the tables so that they can hold rectangles up to the given side length. Known results are kept.
         */
        private void ensureCapacity(int side) {
            if(side < count.length) return;

            int[][] newCount = new int[side + 1][side + 1];
            int[][][] newPlan = new int[side + 1][side + 1][];
            for(int[] column : newCount) {
                Arrays.fill(column, -1);
            }

            for(int width = 0;width < count.length;width++) {
                System.arraycopy(count[width], 0, newCount[width], 0, count[width].length);
                System.arraycopy(plan[width], 0, newPlan[width], 0, plan[width].length);
            }

            count = newCount;
            plan = newPlan;
        }
    }
}
