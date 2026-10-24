package solver.nanofabricator;

import java.util.Scanner;

/**
 * A hex board in its character representation. Neighbouring cells are two columns apart in the same row
 * or one row and one column apart diagonally.
 */
public class Board {
    //row and column step per emitter direction 1-6 (index 0 unused)
    public static final int[] DR = {0, 0, 1, 1, 0, -1, -1};
    public static final int[] DC = {0, 2, 1, -1, -2, -1, 1};

    public final int radius;
    public final int rows;
    public final int cols;
    public final char[][] grid;

    public Board(int radius, int rows, int cols, char[][] grid) {
        this.radius = radius;
        this.rows = rows;
        this.cols = cols;
        this.grid = grid;
    }

    /**
     * Reads a dimension line "radius rows cols" followed by the board lines. Leading empty lines are skipped.
     */
    public static Board read(Scanner reader) {
        String line = reader.nextLine().trim();
        while(line.isEmpty()) {
            line = reader.nextLine().trim();
        }

        String[] dims = line.split("\\s+");
        int radius = Integer.parseInt(dims[0]);
        int rows = Integer.parseInt(dims[1]);
        int cols = Integer.parseInt(dims[2]);

        char[][] grid = new char[rows][];
        for(int r = 0;r < rows;r++) {
            grid[r] = reader.nextLine().trim().toCharArray();
        }

        return new Board(radius, rows, cols, grid);
    }

    public boolean inside(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < grid[r].length;
    }

    /**
     * Fires the emitter at (r, c) in direction dir and turns every 'O' on its way into 'M' until the beam leaves
     * the cells of the board.
     */
    public void fire(int r, int c, int dir) {
        r += DR[dir];
        c += DC[dir];
        while(inside(r, c) && (grid[r][c] == 'O' || grid[r][c] == 'M')) {
            grid[r][c] = 'M';
            r += DR[dir];
            c += DC[dir];
        }
    }

    /**
     * Fires every emitter ('1'-'6') on the board.
     */
    public void fireAll() {
        for(int r = 0;r < rows;r++) {
            for(int c = 0;c < grid[r].length;c++) {
                char ch = grid[r][c];
                if(ch >= '1' && ch <= '6') {
                    fire(r, c, ch - '0');
                }
            }
        }
    }

    public boolean isCell(int r, int c) {
        return inside(r, c) && (grid[r][c] == 'O' || grid[r][c] == 'M');
    }

    /**
     * Creates an empty board of the given radius (no emitters).
     */
    public static Board empty(int radius) {
        String[] lines = Level1.renderBoard(radius).split("\n");
        char[][] grid = new char[lines.length][];
        for(int r = 0;r < lines.length;r++) {
            grid[r] = lines[r].toCharArray();
        }
        return new Board(radius, lines.length, grid[0].length, grid);
    }

    /**
     * The border cells as {row, col}, clockwise starting at the left corner. Corner of emitter d (1-6) is at
     * index (d - 1) * radius.
     */
    public int[][] ring() {
        //walking directions clockwise from the left corner: up-right, right, down-right, down-left, left, up-left
        int[] walk = {6, 1, 2, 3, 4, 5};
        int[][] ring = new int[6 * radius][];
        int r = radius, c = 0, i = 0;
        for(int side = 0;side < 6;side++) {
            for(int step = 0;step < radius;step++) {
                ring[i++] = new int[]{r, c};
                r += DR[walk[side]];
                c += DC[walk[side]];
            }
        }
        return ring;
    }

    /**
     * Emitters found on the board as {row, col, dir}.
     */
    public java.util.List<int[]> emitters() {
        java.util.List<int[]> emitters = new java.util.ArrayList<>();
        for(int r = 0;r < rows;r++) {
            for(int c = 0;c < grid[r].length;c++) {
                char ch = grid[r][c];
                if(ch >= '1' && ch <= '6') emitters.add(new int[]{r, c, ch - '0'});
            }
        }
        return emitters;
    }

    /**
     * Counts for every cell how many beams of the given emitters {row, col, dir} pass through it.
     * Beams pass through 'O' and 'M'.
     */
    public int[][] beamCounts(java.util.List<int[]> emitters) {
        int[][] counts = new int[rows][];
        for(int r = 0;r < rows;r++) {
            counts[r] = new int[grid[r].length];
        }

        for(int[] e : emitters) {
            int dir = e[2];
            int br = e[0] + DR[dir], bc = e[1] + DC[dir];
            while(isCell(br, bc)) {
                counts[br][bc]++;
                br += DR[dir];
                bc += DC[dir];
            }
        }

        return counts;
    }

    /**
     * Fires the given emitters at half power: exactly 2 beams create matter, 3 or more destroy it.
     */
    public void fireHalfPower(java.util.List<int[]> emitters) {
        int[][] counts = beamCounts(emitters);
        for(int r = 0;r < rows;r++) {
            for(int c = 0;c < grid[r].length;c++) {
                if(counts[r][c] == 2) grid[r][c] = 'M';
                else if(counts[r][c] >= 3) grid[r][c] = 'O';
            }
        }
    }

    /**
     * Fires all emitters on the board at half power.
     */
    public void fireAllHalfPower() {
        fireHalfPower(emitters());
    }

    /**
     * All matter cells as {row, col}.
     */
    public java.util.List<int[]> matter() {
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for(int r = 0;r < rows;r++) {
            for(int c = 0;c < grid[r].length;c++) {
                if(grid[r][c] == 'M') cells.add(new int[]{r, c});
            }
        }
        return cells;
    }

    /**
     * True if every given cell is matter and no other matter is adjacent to any of them.
     */
    public boolean containsIsolated(java.util.List<int[]> pattern) {
        java.util.Set<Long> inPattern = new java.util.HashSet<>();
        for(int[] p : pattern) {
            if(!inside(p[0], p[1]) || grid[p[0]][p[1]] != 'M') return false;
            inPattern.add(key(p[0], p[1]));
        }

        for(int[] p : pattern) {
            for(int d = 1;d <= 6;d++) {
                int nr = p[0] + DR[d], nc = p[1] + DC[d];
                if(inside(nr, nc) && grid[nr][nc] == 'M' && !inPattern.contains(key(nr, nc))) return false;
            }
        }
        return true;
    }

    public void clear(java.util.List<int[]> cells) {
        for(int[] p : cells) {
            grid[p[0]][p[1]] = 'O';
        }
    }

    private static long key(int r, int c) {
        return ((long) r << 32) | (c & 0xffffffffL);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(char[] row : grid) {
            sb.append(row).append("\n");
        }
        return sb.toString();
    }
}
