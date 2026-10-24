package solver.nanofabricator;

import java.util.*;

/**
 * Third planner for level 6: two parallel sweepers S1, S2 (an emitter and its twin) and two parallel pens A, B.
 * Every active sweeper/pen pair creates matter at its intersection, so up to 4 cells per tick. Rows are processed
 * in phases of two rows (one per sweeper). Each tick all 144 action combinations of the four emitters are scored
 * greedily; combinations that would create matter outside the pattern are rejected.
 */
public class PairPlanner {
    private final Board pattern;
    private final int count;
    private final int[] sweepers;
    private final int[] pens;
    private final boolean halves;
    private final double moveWeight, offPenalty;

    private final int n;
    private final int vCount, uCount;
    private final int[][][] cellAt;
    private final boolean[][] isPattern;
    private final int[][] sRingOfV = new int[2][];
    private final int[][] penRingOfU = new int[2][];
    private final List<Map<Integer, Integer>> sVOfRing = new ArrayList<>();
    private final List<Map<Integer, Integer>> penUOfRing = new ArrayList<>();
    private final List<int[]> patternCells;
    private final TreeMap<Integer, List<Integer>> rowCells = new TreeMap<>();

    private PairPlanner(Board pattern, int count, int s, int a, boolean halves, double moveWeight, double offPenalty) {
        this.moveWeight = moveWeight;
        this.offPenalty = offPenalty;
        this.pattern = pattern;
        this.count = count;
        this.sweepers = new int[]{s, s > 3 ? s - 3 : s + 3};
        this.pens = new int[]{a, a + 3};
        this.halves = halves;

        Board empty = Board.empty(pattern.radius);
        int[][] ring = empty.ring();
        n = ring.length;

        List<Map<Integer, Integer>> sRings = List.of(Level6.lineToRing(empty, ring, sweepers[0]), Level6.lineToRing(empty, ring, sweepers[1]));
        List<Map<Integer, Integer>> pRings = List.of(Level6.lineToRing(empty, ring, pens[0]), Level6.lineToRing(empty, ring, pens[1]));

        List<Integer> sLines = new ArrayList<>(new TreeSet<>(sRings.get(0).keySet()));
        List<Integer> uLines = new ArrayList<>(new TreeSet<>(pRings.get(0).keySet()));
        vCount = sLines.size();
        uCount = uLines.size();

        for(int i = 0;i < 2;i++) {
            sRingOfV[i] = new int[vCount];
            Map<Integer, Integer> inv = new HashMap<>();
            for(int v = 0;v < vCount;v++) {
                Integer idx = sRings.get(i).get(sLines.get(v));
                if(idx == null) throw new IllegalStateException("sweeper line not reachable");
                sRingOfV[i][v] = idx;
                inv.put(idx, v);
            }
            sVOfRing.add(inv);

            penRingOfU[i] = new int[uCount];
            Map<Integer, Integer> invU = new HashMap<>();
            for(int u = 0;u < uCount;u++) {
                Integer idx = pRings.get(i).get(uLines.get(u));
                if(idx == null) throw new IllegalStateException("pen line not reachable");
                penRingOfU[i][u] = idx;
                invU.put(idx, u);
            }
            penUOfRing.add(invU);
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
        for(int[] p : patternCells) {
            int v = vOfLine.get(Level6.lineId(s, p[0], p[1]));
            int u = uOfLine.get(Level6.lineId(a, p[0], p[1]));
            isPattern[v][u] = true;
            rowCells.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        }
    }

    public static List<String[]> plan(Board pattern, int count, int s, int a, boolean halves, double moveWeight, double offPenalty) {
        try {
            return new PairPlanner(pattern, count, s, a, halves, moveWeight, offPenalty).run();
        } catch(RuntimeException e) {
            return null;
        }
    }

    /**
     * Phases as {row of S1, row of S2 (or -1)} for one copy in forward order.
     */
    private List<int[]> phases() {
        List<Integer> rows = new ArrayList<>(rowCells.keySet());
        List<int[]> phases = new ArrayList<>();
        if(halves) {
            int h = (rows.size() + 1) / 2;
            for(int i = 0;i < h;i++) {
                phases.add(new int[]{rows.get(i), i + h < rows.size() ? rows.get(i + h) : -1});
            }
        } else {
            for(int i = 0;i < rows.size();i += 2) {
                phases.add(new int[]{rows.get(i), i + 1 < rows.size() ? rows.get(i + 1) : -1});
            }
        }
        return phases;
    }

    private boolean[][] done;

    private boolean needed(int v, int u) {
        return isPattern[v][u] && !done[v][u];
    }

    private void snapshot(Board board) {
        done = new boolean[vCount][uCount];
        for(var entry : rowCells.entrySet()) {
            int v = entry.getKey();
            for(int u : entry.getValue()) {
                int[] rc = cellAt[v][u];
                done[v][u] = board.grid[rc[0]][rc[1]] == 'M';
            }
        }
    }

    private boolean phaseDone(int[] phase) {
        for(int k = 0;k < 2;k++) {
            int v = phase[k];
            if(v < 0) continue;
            for(int u : rowCells.get(v)) if(!done[v][u]) return false;
        }
        return true;
    }

    /**
     * Columns of the phase split between the pens, balancing the number of columns with needed cells.
     */
    private int[][] split(int[] phase, boolean onlyNeeded) {
        TreeSet<Integer> cols = new TreeSet<>();
        for(int k = 0;k < 2;k++) {
            int v = phase[k];
            if(v < 0) continue;
            for(int u : rowCells.get(v)) if(!onlyNeeded || !done[v][u]) cols.add(u);
        }
        List<Integer> list = new ArrayList<>(cols);
        int half = (list.size() + 1) / 2;
        return new int[][]{
                list.subList(0, half).stream().mapToInt(Integer::intValue).toArray(),
                list.subList(half, list.size()).stream().mapToInt(Integer::intValue).toArray()
        };
    }

    private List<String[]> run() {
        Machine machine = new Machine(pattern.radius);
        List<String[]> program = new ArrayList<>();
        List<int[]> forward = phases();
        List<int[]> backward = new ArrayList<>(forward);
        Collections.reverse(backward);

        int submitted = 0, copy = 0, phaseIdx = 0;
        int limit = 20 * patternCells.size() * count + 2000;
        int[][] assignment = null;
        int assignedPhase = -1;

        while(submitted < count) {
            if(program.size() > limit) return null;
            snapshot(machine.board);

            List<int[]> order = copy % 2 == 0 ? forward : backward;
            while(phaseIdx < order.size() && phaseDone(order.get(phaseIdx))) phaseIdx++;
            if(phaseIdx >= order.size()) phaseIdx = 0;
            int[] phase = order.get(phaseIdx);
            int[] nextPhase = phaseIdx + 1 < order.size() ? order.get(phaseIdx + 1) : (copy % 2 == 0 ? backward : forward).get(0);

            if(assignedPhase != phaseIdx + copy * 100000) {
                assignment = split(phase, false);
                assignedPhase = phaseIdx + copy * 100000;
            }
            int[][] nextAssignment = split(nextPhase, false);

            //current state
            int[] sv = new int[2];
            boolean[] sOn = new boolean[2];
            int[] pu = new int[2];
            boolean[] pOn = new boolean[2];
            for(int i = 0;i < 2;i++) {
                sv[i] = sVOfRing.get(i).get(machine.pos[sweepers[i]]);
                sOn[i] = machine.active[sweepers[i]];
                pu[i] = penUOfRing.get(i).get(machine.pos[pens[i]]);
                pOn[i] = machine.active[pens[i]];
            }

            //targets
            int[] sTarget = new int[2];
            for(int i = 0;i < 2;i++) {
                sTarget[i] = phase[i] >= 0 ? phase[i] : (nextPhase[i] >= 0 ? nextPhase[i] : sv[i]);
            }
            int[] pTarget = new int[2];
            for(int i = 0;i < 2;i++) {
                int[] rem = Arrays.stream(assignment[i]).filter(u -> columnNeeded(phase, u)).toArray();
                if(rem.length == 0) rem = nextAssignment[i];
                pTarget[i] = rem.length == 0 ? pu[i] : nearerExtreme(rem, pu[i]);
            }

            //options per emitter: {new position, new state, action index}
            List<int[]>[] sOpts = new List[2];
            List<int[]>[] pOpts = new List[2];
            for(int i = 0;i < 2;i++) {
                sOpts[i] = new ArrayList<>();
                sOpts[i].add(new int[]{sv[i], sOn[i] ? 1 : 0, 0});
                sOpts[i].add(new int[]{sv[i], sOn[i] ? 0 : 1, 1});
                if(sv[i] != sTarget[i]) sOpts[i].add(new int[]{sv[i] + Integer.signum(sTarget[i] - sv[i]), sOn[i] ? 1 : 0, 2});

                pOpts[i] = new ArrayList<>();
                pOpts[i].add(new int[]{pu[i], pOn[i] ? 1 : 0, 0});
                pOpts[i].add(new int[]{pu[i], pOn[i] ? 0 : 1, 1});
                if(pu[i] + 1 < uCount) pOpts[i].add(new int[]{pu[i] + 1, pOn[i] ? 1 : 0, 2});
                if(pu[i] - 1 >= 0) pOpts[i].add(new int[]{pu[i] - 1, pOn[i] ? 1 : 0, 3});
            }

            double bestScore = Double.MAX_VALUE;
            int[][] best = null;
            for(int[] s0 : sOpts[0]) for(int[] s1 : sOpts[1]) for(int[] p0 : pOpts[0]) for(int[] p1 : pOpts[1]) {
                int[][] combo = {s0, s1, p0, p1};
                double score = evaluate(combo, phase, sTarget, pTarget);
                if(score < bestScore) {
                    bestScore = score;
                    best = combo;
                }
            }
            if(best == null) return null;

            String[] line = {"-", "-", "-", "-", "-", "-"};
            for(int i = 0;i < 2;i++) {
                line[sweepers[i] - 1] = action(best[i][2], sRingOfV[i], sv[i], best[i][0]);
                line[pens[i] - 1] = action(best[2 + i][2], penRingOfU[i], pu[i], best[2 + i][0]);
            }

            machine.tick(line);
            program.add(line);

            if(machine.board.containsIsolated(patternCells)) {
                machine.board.clear(patternCells);
                submitted++;
                copy++;
                phaseIdx = 0;
                assignedPhase = -1;
            }
        }
        return program;
    }

    private boolean columnNeeded(int[] phase, int u) {
        for(int k = 0;k < 2;k++) {
            if(phase[k] >= 0 && needed(phase[k], u)) return true;
        }
        return false;
    }

    private double evaluate(int[][] combo, int[] phase, int[] sTarget, int[] pTarget) {
        int v0 = combo[0][0], v1 = combo[1][0];
        boolean on0 = combo[0][1] == 1, on1 = combo[1][1] == 1;
        int u0 = combo[2][0], u1 = combo[3][0];
        boolean pon0 = combo[2][1] == 1, pon1 = combo[3][1] == 1;

        if(on0 && on1 && v0 == v1) return Double.MAX_VALUE;
        if(pon0 && pon1 && u0 == u1) return Double.MAX_VALUE;

        int created = 0;
        int[] vs = {v0, v1};
        boolean[] sOns = {on0, on1};
        int[] us = {u0, u1};
        boolean[] pOns = {pon0, pon1};
        for(int i = 0;i < 2;i++) {
            if(!sOns[i]) continue;
            for(int j = 0;j < 2;j++) {
                if(!pOns[j]) continue;
                if(!isPattern[vs[i]][us[j]]) return Double.MAX_VALUE;
                if(!done[vs[i]][us[j]]) created++;
            }
        }

        double score = -10.0 * created;
        for(int i = 0;i < 2;i++) {
            score += Math.abs(vs[i] - sTarget[i]) * moveWeight;
            score += Math.abs(us[i] - pTarget[i]);
            //an emitter that is off where it has work left needs a toggle later
            if(!sOns[i] && phase[i] >= 0 && vs[i] == phase[i] && rowNeeded(phase[i])) score += offPenalty;
            if(!pOns[i] && us[i] == pTarget[i] && columnNeeded(phase, us[i])) score += offPenalty;
            if(combo[i][2] != 0) score += 0.01;
            if(combo[2 + i][2] != 0) score += 0.01;
        }
        return score;
    }

    private boolean rowNeeded(int v) {
        for(int u : rowCells.get(v)) if(!done[v][u]) return true;
        return false;
    }

    private String action(int kind, int[] ringOf, int from, int to) {
        if(kind == 0) return "-";
        if(kind == 1) return "T";
        int forward = ((ringOf[to] - ringOf[from]) % n + n) % n;
        if(forward == 1) return "F";
        if(forward == n - 1) return "B";
        throw new IllegalStateException("lines are not adjacent on the ring");
    }

    private static int nearerExtreme(int[] sorted, int u) {
        int lo = sorted[0], hi = sorted[sorted.length - 1];
        return Math.abs(u - lo) <= Math.abs(u - hi) ? lo : hi;
    }
}
