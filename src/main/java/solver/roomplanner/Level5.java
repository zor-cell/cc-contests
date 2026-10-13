package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * A 1x2 desk plus the free cells it needs to its right and below covers a 2x3 block. Two desks touch exactly when
 * their blocks overlap, so placing non-touching desks in an x by y room is the same as packing 2x3 blocks into an
 * (x + 1) by (y + 1) area (the extra row and column account for desks that touch the right and bottom wall).
 */
public class Level5 {
    private static final Logger log = CustomLogger.getLogger();

    private static final int DESK_LENGTH = 2;
    private static final int BLOCK_SHORT = DESK_LENGTH;
    private static final int BLOCK_LONG = DESK_LENGTH + 1;
    private static final int BLOCK_AREA = BLOCK_SHORT * BLOCK_LONG;

    //strips whose length is a multiple of this can be packed without any gap, whatever their width is
    private static final int STRIP_UNIT = BLOCK_AREA;
    //the part of the area that is solved by search has sides in [MIN_CORE, MIN_CORE + STRIP_UNIT)
    private static final int MIN_CORE = 5;

    //core width -> core height -> blocks of the core, each as {row, col, horizontal ? 1 : 0}
    private static final Map<Integer, Map<Integer, List<int[]>>> coreCache = new HashMap<>();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int x = reader.nextInt();
            int y = reader.nextInt();
            int desks = reader.nextInt();

            boolean[][] room = new boolean[y][x];
            int placed = placeDesks(room, x, y);

            if(placed != desks) {
                log.severe("Run " + run + ": placed " + placed + " desks in " + x + "x" + y + ", but " + desks + " are required");
            }

            //write result to file
            writer.write(format(room) + "\n");
        }
    }

    /**
     * Places the maximum number of non-touching desks in the room and returns how many were placed.
     */
    private static int placeDesks(boolean[][] room, int x, int y) {
        int width = x + 1;
        int height = y + 1;

        int coreWidth = coreLength(width);
        int coreHeight = coreLength(height);

        List<int[]> blocks = new ArrayList<>(solveCore(coreWidth, coreHeight));
        //right of the core, then below the core and the right strip
        fillStrip(blocks, 0, coreWidth, coreHeight, width - coreWidth);
        fillStrip(blocks, coreHeight, 0, height - coreHeight, width);

        for(int[] block : blocks) {
            boolean horizontal = block[2] == 1;
            for(int i = 0;i < DESK_LENGTH;i++) {
                room[block[0] + (horizontal ? 0 : i)][block[1] + (horizontal ? i : 0)] = true;
            }
        }

        return blocks.size();
    }

    /**
     * Shortens a side by whole strip units, down to a length the core search can handle.
     */
    private static int coreLength(int length) {
        return MIN_CORE + (length - MIN_CORE) % STRIP_UNIT;
    }

    /**
     * Packs the area with the given top left corner completely. One of its sides has to be a multiple of
     * {@link #STRIP_UNIT} (or the area has to be empty), the other one at least {@link #BLOCK_SHORT}.
     */
    private static void fillStrip(List<int[]> blocks, int fromRow, int fromCol, int height, int width) {
        if(height == 0 || width == 0) return;

        if(width % STRIP_UNIT == 0) {
            //bands of rows: an odd height gets one band of upright blocks, the rest are bands of lying blocks
            int row = fromRow;
            if(height % BLOCK_SHORT != 0) {
                for(int col = fromCol;col < fromCol + width;col += BLOCK_SHORT) {
                    blocks.add(new int[]{row, col, 0});
                }
                row += BLOCK_LONG;
            }
            for(;row < fromRow + height;row += BLOCK_SHORT) {
                for(int col = fromCol;col < fromCol + width;col += BLOCK_LONG) {
                    blocks.add(new int[]{row, col, 1});
                }
            }
        } else {
            //bands of columns: an odd width gets one band of lying blocks, the rest are bands of upright blocks
            int col = fromCol;
            if(width % BLOCK_SHORT != 0) {
                for(int row = fromRow;row < fromRow + height;row += BLOCK_SHORT) {
                    blocks.add(new int[]{row, col, 1});
                }
                col += BLOCK_LONG;
            }
            for(;col < fromCol + width;col += BLOCK_SHORT) {
                for(int row = fromRow;row < fromRow + height;row += BLOCK_LONG) {
                    blocks.add(new int[]{row, col, 0});
                }
            }
        }
    }

    /**
     * Finds a packing of the core area with as many blocks as possible by backtracking.
     */
    private static List<int[]> solveCore(int width, int height) {
        Map<Integer, List<int[]>> byHeight = coreCache.computeIfAbsent(width, k -> new HashMap<>());
        List<int[]> cached = byHeight.get(height);
        if(cached != null) return cached;

        //allow more and more wasted cells until a packing exists
        int waste = width * height % BLOCK_AREA;
        List<int[]> blocks = new ArrayList<>();
        while(!search(new boolean[height][width], 0, waste, blocks)) {
            waste += BLOCK_AREA;
        }

        byHeight.put(height, blocks);
        return blocks;
    }

    /**
     * Covers the first free cell (in reading order) with the top left corner of a block or leaves it empty, as long
     * as there are wasted cells left.
     */
    private static boolean search(boolean[][] used, int from, int wasteLeft, List<int[]> blocks) {
        int height = used.length;
        int width = used[0].length;

        int pos = from;
        while(pos < width * height && used[pos / width][pos % width]) pos++;
        if(pos == width * height) return true;

        int row = pos / width;
        int col = pos % width;

        for(int horizontal = 1;horizontal >= 0;horizontal--) {
            int blockHeight = horizontal == 1 ? BLOCK_SHORT : BLOCK_LONG;
            int blockWidth = horizontal == 1 ? BLOCK_LONG : BLOCK_SHORT;
            if(!isFree(used, row, col, blockHeight, blockWidth)) continue;

            mark(used, row, col, blockHeight, blockWidth, true);
            blocks.add(new int[]{row, col, horizontal});
            if(search(used, pos + 1, wasteLeft, blocks)) return true;
            blocks.remove(blocks.size() - 1);
            mark(used, row, col, blockHeight, blockWidth, false);
        }

        if(wasteLeft > 0) {
            used[row][col] = true;
            if(search(used, pos + 1, wasteLeft - 1, blocks)) return true;
            used[row][col] = false;
        }

        return false;
    }

    private static boolean isFree(boolean[][] used, int row, int col, int blockHeight, int blockWidth) {
        if(row + blockHeight > used.length || col + blockWidth > used[0].length) return false;

        for(int r = row;r < row + blockHeight;r++) {
            for(int c = col;c < col + blockWidth;c++) {
                if(used[r][c]) return false;
            }
        }

        return true;
    }

    private static void mark(boolean[][] used, int row, int col, int blockHeight, int blockWidth, boolean value) {
        for(int r = row;r < row + blockHeight;r++) {
            for(int c = col;c < col + blockWidth;c++) {
                used[r][c] = value;
            }
        }
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
}
