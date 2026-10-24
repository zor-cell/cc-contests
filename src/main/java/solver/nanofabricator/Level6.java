package solver.nanofabricator;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Builds programs with one sweeping emitter S that is always active and two parallel pen emitters A and B.
 * The pens never intersect each other, so matter is only created where S crosses an active pen. A pass moves S
 * across the lines it needs while each pen sits on one pattern line and toggles on exactly at the needed cells.
 */
public class Level6 {
    private static final Logger log = CustomLogger.getLogger();

    //{sweeper move weight, penalty for being off where work is left} for the pair planner
    private static final double[][] PAIR_WEIGHTS = {{1.5, 0.6}, {1.0, 0.3}, {2.5, 1.0}, {1.5, 0.0}};

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = Integer.parseInt(Level4.nextNonEmpty(reader));

        long total = 0;
        for(int run = 0;run < maxRuns;run++) {
            String[] info = Level4.nextNonEmpty(reader).split("\\s+");
            int radius = Integer.parseInt(info[0]);
            int rows = Integer.parseInt(info[1]);
            int cols = Integer.parseInt(info[2]);
            int count = Integer.parseInt(info[3]);

            char[][] grid = new char[rows][];
            for(int r = 0;r < rows;r++) {
                grid[r] = reader.nextLine().trim().toCharArray();
            }
            Board pattern = new Board(radius, rows, cols, grid);

            List<String[]> best = bestProgram(pattern, count);
            if(best == null) {
                log.severe("Run " + run + ": no valid program found");
                best = new ArrayList<>();
            }
            total += best.size();

            for(String[] line : best) {
                writer.write(String.join(" ", line) + "\n");
            }
            writer.write("\n");
        }
        log.info("Total lines: " + total);
    }

    /**
     * Feeds every pattern (header + board) and its program into the sandbox.
     */
    public static void verify(Scanner input, Scanner output, solver.SolutionVerifier verifier) throws IOException {
        int maxRuns = Integer.parseInt(Level4.nextNonEmpty(input));

        for(int run = 0;run < maxRuns;run++) {
            String header = Level4.nextNonEmpty(input);
            int rows = Integer.parseInt(header.split("\\s+")[1]);
            StringBuilder inputCase = new StringBuilder(header);
            for(int r = 0;r < rows;r++) {
                inputCase.append("\n").append(input.nextLine().trim());
            }

            StringBuilder outputCase = new StringBuilder();
            while(output.hasNextLine()) {
                String line = output.nextLine().trim();
                if(line.isEmpty()) break;
                if(!outputCase.isEmpty()) outputCase.append("\n");
                outputCase.append(line);
            }

            verifier.checkValidity(run, inputCase.toString(), outputCase.toString());
        }
    }

    /**
     * Tries every sweeper / pen axis combination and returns the shortest program that is verified by simulation.
     */
    public static List<String[]> bestProgram(Board pattern, int count) {
        List<String[]> best = null;
        for(int s = 1;s <= 6;s++) {
            for(int a = 1;a <= 3;a++) {
                int b = a + 3;
                if(axis(a) == axis(s)) continue;

                List<List<String[]>> candidates = new ArrayList<>();
                candidates.add(plan(pattern, count, s, a, b));
                candidates.add(CursorPlanner.plan(pattern, count, s, a, b));
                for(double[] w : PAIR_WEIGHTS) {
                    candidates.add(PairPlanner.plan(pattern, count, s, a, false, w[0], w[1]));
                    candidates.add(PairPlanner.plan(pattern, count, s, a, true, w[0], w[1]));
                }
                for(List<String[]> program : candidates) {
                    if(program == null) continue;
                    if(best != null && program.size() >= best.size()) continue;

                    String[][] arr = program.toArray(new String[0][]);
                    if(Level5.countSubmissions(pattern, arr) >= count) {
                        best = program;
                    }
                }
            }
        }
        return best;
    }

    static int axis(int dir) {
        return (dir - 1) % 3;
    }

    /**
     * Id of the line along the axis of {@code dir} through the cell (r, c).
     */
    static int lineId(int dir, int r, int c) {
        return switch(axis(dir)) {
            case 0 -> r;
            case 1 -> c - r;
            default -> c + r;
        };
    }

    /**
     * Maps every line of the axis of emitter {@code e} that contains board cells to the ring index from which
     * the emitter's beam covers this line.
     */
    static Map<Integer, Integer> lineToRing(Board empty, int[][] ring, int e) {
        Map<Integer, Integer> map = new HashMap<>();
        for(int i = 0;i < ring.length;i++) {
            int r = ring[i][0] + Board.DR[e], c = ring[i][1] + Board.DC[e];
            if(empty.isCell(r, c)) {
                map.putIfAbsent(lineId(e, r, c), i);
            }
        }
        return map;
    }

    private record Job(int pass, int line, Set<Integer> needed) {}

    static List<String[]> plan(Board pattern, int count, int s, int penA, int penB) {
        int radius = pattern.radius;
        Board empty = Board.empty(radius);
        int[][] ring = empty.ring();
        int n = ring.length;

        Map<Integer, Integer> sRing = lineToRing(empty, ring, s);
        Map<Integer, Integer> aRing = lineToRing(empty, ring, penA);
        Map<Integer, Integer> bRing = lineToRing(empty, ring, penB);

        //pen line -> sweeper lines that need a cell
        TreeMap<Integer, Set<Integer>> cells = new TreeMap<>();
        for(int[] p : pattern.matter()) {
            int x = lineId(s, p[0], p[1]);
            int y = lineId(penA, p[0], p[1]);
            if(!sRing.containsKey(x) || !aRing.containsKey(y) || !bRing.containsKey(y)) return null;
            cells.computeIfAbsent(y, k -> new HashSet<>()).add(x);
        }
        if(cells.isEmpty()) return null;

        //passes: two pen lines each, alternating line order per copy
        List<Integer> asc = new ArrayList<>(cells.keySet());
        List<Integer> desc = new ArrayList<>(asc);
        Collections.reverse(desc);

        List<Job> jobsA = new ArrayList<>();
        List<Job> jobsB = new ArrayList<>();
        List<Set<Integer>> passNeeds = new ArrayList<>();
        for(int copy = 0;copy < count;copy++) {
            List<Integer> order = copy % 2 == 0 ? asc : desc;
            for(int i = 0;i < order.size();i += 2) {
                int pass = passNeeds.size();
                Set<Integer> union = new HashSet<>(cells.get(order.get(i)));
                jobsA.add(new Job(pass, order.get(i), cells.get(order.get(i))));
                if(i + 1 < order.size()) {
                    jobsB.add(new Job(pass, order.get(i + 1), cells.get(order.get(i + 1))));
                    union.addAll(cells.get(order.get(i + 1)));
                }
                passNeeds.add(union);
            }
        }

        //state
        int[] pos = new int[7];
        boolean[] active = new boolean[7];
        for(int d = 1;d <= 6;d++) {
            pos[d] = (d - 1) * radius;
        }

        //ring index -> sweeper line (or null)
        Integer[] sLineAt = new Integer[n];
        for(var entry : sRing.entrySet()) {
            sLineAt[entry.getValue()] = entry.getKey();
        }

        List<String[]> program = new ArrayList<>();
        int[] jobIdx = new int[7];
        int[] pens = {penA, penB};
        Map<Integer, List<Job>> jobs = Map.of(penA, jobsA, penB, jobsB);
        Map<Integer, Map<Integer, Integer>> penRing = Map.of(penA, aRing, penB, bRing);

        for(int pass = 0;pass < passNeeds.size();pass++) {
            Set<Integer> union = passNeeds.get(pass);
            int xMin = Collections.min(union), xMax = Collections.max(union);
            int iMin = sRing.get(xMin), iMax = sRing.get(xMax);

            int start = ringDist(pos[s], iMin, n) <= ringDist(pos[s], iMax, n) ? iMin : iMax;
            int end = start == iMin ? iMax : iMin;

            List<Integer> route = new ArrayList<>();
            route.add(pos[s]);
            appendWalk(route, pos[s], start, n);
            appendWalk(route, start, end, n);

            //last route index each current pen needs
            Map<Integer, Integer> lastNeeded = new HashMap<>();
            for(int p : pens) {
                Job job = currentJob(jobs.get(p), jobIdx[p]);
                if(job == null || job.pass != pass) continue;
                int last = -1;
                for(int j = 0;j < route.size();j++) {
                    Integer x = sLineAt[route.get(j)];
                    if(x != null && job.needed.contains(x)) last = j;
                }
                lastNeeded.put(p, last);
            }

            int j = 0;
            int processed = -1;
            int guard = 0;
            while(processed < route.size() - 1) {
                if(++guard > 100000) return null;

                //sweeper decision based on the state at the start of the tick
                String[] line = {"-", "-", "-", "-", "-", "-"};
                int target = processed == j ? j + 1 : j;
                boolean ready = target < route.size() && isReady(route.get(target), sLineAt, pens, jobs, jobIdx, pass, penRing, pos);

                if(!active[s]) {
                    line[s - 1] = "T";
                    active[s] = true;
                    if(target == j && ready) processed = j;
                } else if(ready) {
                    if(target != j) {
                        line[s - 1] = step(pos[s], route.get(target), n) > 0 ? "F" : "B";
                        pos[s] = route.get(target);
                        j = target;
                    }
                    processed = j;
                }

                Integer x = sLineAt[pos[s]];

                //pens
                for(int p : pens) {
                    List<Job> list = jobs.get(p);
                    Job job = currentJob(list, jobIdx[p]);
                    //finished jobs of this pass move on to the next one
                    while(job != null && job.pass == pass && processed >= lastNeeded.getOrDefault(p, -1) && !job.needed.contains(x)) {
                        jobIdx[p]++;
                        job = currentJob(list, jobIdx[p]);
                    }

                    if(job == null) {
                        if(active[p]) toggle(line, active, p);
                        continue;
                    }

                    int goal = penRing.get(p).get(job.line);
                    if(pos[p] != goal) {
                        if(active[p]) {
                            toggle(line, active, p);
                        } else {
                            int dir = step(pos[p], goal, n);
                            line[p - 1] = dir > 0 ? "F" : "B";
                            pos[p] = (pos[p] + dir + n) % n;
                        }
                        continue;
                    }

                    boolean desired = job.pass == pass && x != null && job.needed.contains(x);
                    if(active[p] != desired) toggle(line, active, p);
                }

                program.add(line);
            }

            //jobs of this pass are done
            for(int p : pens) {
                Job job = currentJob(jobs.get(p), jobIdx[p]);
                if(job != null && job.pass == pass) jobIdx[p]++;
            }
        }

        return program;
    }

    private static boolean isReady(int ringIdx, Integer[] sLineAt, int[] pens, Map<Integer, List<Job>> jobs, int[] jobIdx,
                                   int pass, Map<Integer, Map<Integer, Integer>> penRing, int[] pos) {
        Integer x = sLineAt[ringIdx];
        if(x == null) return true;
        for(int p : pens) {
            Job job = currentJob(jobs.get(p), jobIdx[p]);
            if(job == null || job.pass != pass || !job.needed.contains(x)) continue;
            if(pos[p] != penRing.get(p).get(job.line)) return false;
        }
        return true;
    }

    private static Job currentJob(List<Job> list, int idx) {
        return idx < list.size() ? list.get(idx) : null;
    }

    private static void toggle(String[] line, boolean[] active, int p) {
        line[p - 1] = "T";
        active[p] = !active[p];
    }

    static int ringDist(int a, int b, int n) {
        int d = Math.abs(a - b);
        return Math.min(d, n - d);
    }

    /**
     * +1 (clockwise, F) or -1 (counterclockwise, B) for the shorter way from a to b, 0 if equal.
     */
    static int step(int a, int b, int n) {
        if(a == b) return 0;
        int forward = ((b - a) % n + n) % n;
        return forward <= n - forward ? 1 : -1;
    }

    /**
     * Appends the ring indices after {@code from} up to and including {@code to}, walking the shorter way.
     */
    static void appendWalk(List<Integer> route, int from, int to, int n) {
        int dir = step(from, to, n);
        int cur = from;
        while(cur != to) {
            cur = (cur + dir + n) % n;
            route.add(cur);
        }
    }
}
