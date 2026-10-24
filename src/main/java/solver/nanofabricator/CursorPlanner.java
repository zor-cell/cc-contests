package solver.nanofabricator;

import java.util.*;

/**
 * Second planner for level 6. The always-active sweeper S and an active pen form a cursor at their intersection.
 * In (v = sweeper line, u = pen line) coordinates the cursor moves like a king: S moves v, the pen moves u.
 * With two parallel pens there are two cursors sharing v, so each sweeper line is split between them and drawn in
 * parallel. The program is simulated tick by tick on a {@link Machine}, so the board state is always exact.
 */
public class CursorPlanner {
    private final Board pattern;
    private final int count;
    private final int s;
    private final int[] pens;

    private final int n;
    private final int vCount, uCount;
    //(v, u) -> {r, c} of the board cell, or null
    private final int[][][] cellAt;
    private final boolean[][] isPattern;
    private final int[] sRingOfV;
    private final int[][] penRingOfU;
    private final Map<Integer, Integer> sVOfRing = new HashMap<>();
    private final List<Map<Integer, Integer>> penUOfRing = new ArrayList<>();

    //per pen: v -> sorted u of the cells it is assigned to
    private final List<List<int[]>> assigned = new ArrayList<>();
    private int vMin = Integer.MAX_VALUE, vMax = Integer.MIN_VALUE;
    private final List<int[]> patternCells;

    private CursorPlanner(Board pattern, int count, int s, int a, int b) {
        this.pattern = pattern;
        this.count = count;
        this.s = s;
        this.pens = new int[]{a, b};

        Board empty = Board.empty(pattern.radius);
        int[][] ring = empty.ring();
        n = ring.length;

        Map<Integer, Integer> sRing = Level6.lineToRing(empty, ring, s);
        Map<Integer, Integer> aRing = Level6.lineToRing(empty, ring, a);
        Map<Integer, Integer> bRing = Level6.lineToRing(empty, ring, b);

        List<Integer> sLines = new ArrayList<>(new TreeSet<>(sRing.keySet()));
        List<Integer> uLines = new ArrayList<>(new TreeSet<>(aRing.keySet()));
        vCount = sLines.size();
        uCount = uLines.size();

        sRingOfV = new int[vCount];
        for(int v = 0;v < vCount;v++) {
            sRingOfV[v] = sRing.get(sLines.get(v));
            sVOfRing.put(sRingOfV[v], v);
        }
        penRingOfU = new int[2][uCount];
        for(int i = 0;i < 2;i++) {
            Map<Integer, Integer> map = i == 0 ? aRing : bRing;
            Map<Integer, Integer> inverse = new HashMap<>();
            for(int u = 0;u < uCount;u++) {
                Integer idx = map.get(uLines.get(u));
                penRingOfU[i][u] = idx == null ? -1 : idx;
                if(idx != null) inverse.put(idx, u);
            }
            penUOfRing.add(inverse);
        }

        Map<Integer, Integer> vOfLine = new HashMap<>(), uOfLine = new HashMap<>();
        for(int v = 0;v < vCount;v++) vOfLine.put(sLines.get(v), v);
        for(int u = 0;u < uCount;u++) uOfLine.put(uLines.get(u), u);

        cellAt = new int[vCount][uCount][];
        isPattern = new boolean[vCount][uCount];
        for(int r = 0;r < empty.rows;r++) {
            for(int c = 0;c < empty.grid[r].length;c++) {
                if(!empty.isCell(r, c)) continue;
                Integer v = vOfLine.get(Level6.lineId(s, r, c));
                Integer u = uOfLine.get(Level6.lineId(a, r, c));
                if(v != null && u != null) cellAt[v][u] = new int[]{r, c};
            }
        }

        patternCells = pattern.matter();
        Map<Integer, List<Integer>> rowCells = new TreeMap<>();
        for(int[] p : patternCells) {
            int v = vOfLine.get(Level6.lineId(s, p[0], p[1]));
            int u = uOfLine.get(Level6.lineId(a, p[0], p[1]));
            isPattern[v][u] = true;
            rowCells.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
            vMin = Math.min(vMin, v);
            vMax = Math.max(vMax, v);
        }

        //split each row between the two cursors so that the longer of the two spans is minimal
        for(int i = 0;i < 2;i++) {
            List<int[]> perRow = new ArrayList<>();
            for(int v = 0;v < vCount;v++) perRow.add(new int[0]);
            assigned.add(perRow);
        }
        for(var entry : rowCells.entrySet()) {
            List<Integer> us = entry.getValue();
            Collections.sort(us);
            int k = us.size();
            int bestSplit = k, bestCost = Integer.MAX_VALUE;
            for(int split = 0;split <= k;split++) {
                int costA = split == 0 ? 0 : spanCost(us, 0, split);
                int costB = split == k ? 0 : spanCost(us, split, k);
                int cost = Math.max(costA, costB);
                if(cost < bestCost) {
                    bestCost = cost;
                    bestSplit = split;
                }
            }
            int[] partA = us.subList(0, bestSplit).stream().mapToInt(Integer::intValue).toArray();
            int[] partB = us.subList(bestSplit, k).stream().mapToInt(Integer::intValue).toArray();
            assigned.get(0).set(entry.getKey(), partA);
            assigned.get(1).set(entry.getKey(), partB);
        }
    }

    //ticks to draw cells from..to-1 of a sorted row: one per cell step plus 2 per gap (toggle off and on)
    private static int spanCost(List<Integer> us, int from, int to) {
        int cost = 1;
        for(int i = from + 1;i < to;i++) {
            int d = us.get(i) - us.get(i - 1);
            cost += d == 1 ? 1 : d + 2;
        }
        return cost;
    }

    /**
     * Debug: {sum over rows of the slower cursor's span cost, number of rows}.
     */
    public static long[] lowerBound(Board pattern, int s, int a, int b) {
        CursorPlanner planner = new CursorPlanner(pattern, 1, s, a, b);
        long sum = 0, rows = 0;
        for(int v = planner.vMin;v <= planner.vMax;v++) {
            List<Integer> la = Arrays.stream(planner.assigned.get(0).get(v)).boxed().toList();
            List<Integer> lb = Arrays.stream(planner.assigned.get(1).get(v)).boxed().toList();
            sum += Math.max(la.isEmpty() ? 0 : spanCost(la, 0, la.size()), lb.isEmpty() ? 0 : spanCost(lb, 0, lb.size()));
            rows++;
        }
        return new long[]{sum, rows};
    }

    public static List<String[]> plan(Board pattern, int count, int s, int a, int b) {
        try {
            return new CursorPlanner(pattern, count, s, a, b).run();
        } catch(RuntimeException e) {
            return null;
        }
    }

    private boolean[][] doneSnapshot(Board board) {
        boolean[][] done = new boolean[vCount][uCount];
        for(int v = vMin;v <= vMax;v++) {
            for(int u = 0;u < uCount;u++) {
                if(isPattern[v][u]) {
                    int[] rc = cellAt[v][u];
                    done[v][u] = board.grid[rc[0]][rc[1]] == 'M';
                }
            }
        }
        return done;
    }

    private int[] remaining(int pen, int v, boolean[][] done) {
        if(v < vMin || v > vMax) return new int[0];
        return Arrays.stream(assigned.get(pen).get(v)).filter(u -> !done[v][u]).toArray();
    }

    private List<String[]> run() {
        Machine machine = new Machine(pattern.radius);
        List<String[]> program = new ArrayList<>();

        int startV = sVOfRing.get(machine.pos[s]);
        int curRow = Math.abs(startV - vMin) <= Math.abs(startV - vMax) ? vMin : vMax;
        int dir = curRow == vMin ? 1 : -1;
        if(vMin == vMax) dir = 1;

        int submitted = 0;
        int limit = 50 * patternCells.size() * count + 2000;

        while(submitted < count) {
            if(program.size() > limit) return null;
            boolean[][] done = doneSnapshot(machine.board);
            String[] line = {"-", "-", "-", "-", "-", "-"};

            //sweeper
            int sv = sVOfRing.get(machine.pos[s]);
            boolean sOn = machine.active[s];
            boolean traveling = sv != curRow;
            if(!sOn) {
                line[s - 1] = "T";
                sOn = true;
            } else if(traveling) {
                sv = moveS(line, sv, curRow);
            } else if(remaining(0, curRow, done).length == 0 && remaining(1, curRow, done).length == 0) {
                int next = curRow + dir;
                if(next < vMin || next > vMax) {
                    dir = -dir;
                    next = curRow + dir;
                    if(next < vMin || next > vMax) next = curRow;
                }
                curRow = next;
                if(sv != curRow) sv = moveS(line, sv, curRow);
            }
            traveling = sv != curRow;

            //pens, decided after the sweeper so they know its line during this tick
            int[] newU = new int[2];
            boolean[] newOn = new boolean[2];
            for(int i = 0;i < 2;i++) {
                int p = pens[i];
                newU[i] = penUOfRing.get(i).get(machine.pos[p]);
                newOn[i] = machine.active[p];
            }

            for(int i = 0;i < 2;i++) {
                int p = pens[i];
                int u = newU[i];
                boolean on = newOn[i];
                int other = 1 - i;

                Integer goal = null;
                boolean goalActive = false;
                if(!traveling) {
                    int[] rem = remaining(i, sv, done);
                    if(rem.length > 0) {
                        goal = nearerExtreme(rem, u);
                        goalActive = true;
                    }
                }
                if(goal == null) {
                    int preRow = traveling ? curRow : curRow + dir;
                    if(preRow < vMin || preRow > vMax) preRow = curRow - dir;
                    int[] rem = remaining(i, preRow, done);
                    if(rem.length == 0) rem = assigned.get(i).get(Math.max(vMin, Math.min(vMax, preRow)));
                    if(rem.length > 0) goal = nearerExtreme(rem, u);
                }

                if(goal == null || (u == goal && !goalActive)) {
                    if(on && !valid(sv, sOn, u, true, newU[other], newOn[other])) {
                        line[p - 1] = "T";
                        on = false;
                    }
                } else if(u == goal) {
                    if(!on && valid(sv, sOn, u, true, newU[other], newOn[other])) {
                        line[p - 1] = "T";
                        on = true;
                    } else if(on && !valid(sv, sOn, u, true, newU[other], newOn[other])) {
                        line[p - 1] = "T";
                        on = false;
                    }
                } else {
                    int step = goal > u ? 1 : -1;
                    int next = u + step;
                    if(on && !valid(sv, sOn, next, true, newU[other], newOn[other])) {
                        line[p - 1] = "T";
                        on = false;
                    } else {
                        line[p - 1] = ringStep(penRingOfU[i][u], penRingOfU[i][next]);
                        u = next;
                    }
                }

                newU[i] = u;
                newOn[i] = on;
            }

            machine.tick(line);
            program.add(line);

            if(machine.board.containsIsolated(patternCells)) {
                machine.board.clear(patternCells);
                submitted++;
            }
        }

        return program;
    }

    private int moveS(String[] line, int sv, int target) {
        int next = sv + (target > sv ? 1 : -1);
        line[s - 1] = ringStep(sRingOfV[sv], sRingOfV[next]);
        return next;
    }

    /**
     * Whether a pen on line u with the given state is allowed while the sweeper is on line v: an active pen may only
     * meet the active sweeper in a pattern cell and must never share its line with the other active pen.
     */
    private boolean valid(int v, boolean sOn, int u, boolean on, int otherU, boolean otherOn) {
        if(!on) return true;
        if(otherOn && otherU == u) return false;
        if(!sOn) return true;
        return v >= 0 && v < vCount && isPattern[v][u];
    }

    private String ringStep(int from, int to) {
        if(from < 0 || to < 0) throw new IllegalStateException("line not reachable");
        int forward = ((to - from) % n + n) % n;
        if(forward == 1) return "F";
        if(forward == n - 1) return "B";
        throw new IllegalStateException("lines are not adjacent on the ring");
    }

    private static int nearerExtreme(int[] sorted, int u) {
        int lo = sorted[0], hi = sorted[sorted.length - 1];
        return Math.abs(u - lo) <= Math.abs(u - hi) ? lo : hi;
    }
}
