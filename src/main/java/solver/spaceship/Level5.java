package solver.spaceship;

import log.CustomLogger;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.lang.Math;

public class Level5 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine();

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            String line = reader.nextLine();
            String[] split = line.split(" ");
            Point s = new Point(split[0]);
            int limit = Integer.parseInt(split[1]);

            String aLine = reader.nextLine();
            Point a = new Point(aLine);

            var xPath = createPath(s.x);
            var yPath = createPath(s.y);

            //var xnormPath = createNormalizedPath(xPath);
            //var ynormPath = createNormalizedPath(yPath);

            //find collision
            /*int t = 0;
            int tx = 0;
            int ty = 0;
            int x = 0;
            int y = 0;
            xPath.remove(0);
            xPath.remove(xPath.size() - 1);
            yPath.remove(0);
            yPath.remove(yPath.size() - 1);
            boolean isCollision = false;
            while(t < limit) {
                int xPace = x >= xPath.size() - 1 ? 0 : Math.abs(xPath.get(x));
                int yPace = y >= yPath.size() - 1 ? 0 : Math.abs(yPath.get(y));

                //update positions
                if(tx <= t && Math.abs(xPace) > 0 && x < xPath.size()) {
                    x++;
                    tx += xPace == 0 ? 1 : xPace;
                }
                if(ty <= t && Math.abs(yPace) > 0 && y < yPath.size()) {
                    y++;
                    ty += yPace == 0 ? 1 : yPace;
                }

                //collision check
                if(Math.abs(Math.abs(a.x) - x) <= 3 && Math.abs(Math.abs(a.y) - y) <= 3) {
                    isCollision = true;
                    break;
                }

                //update timestamps
                t++;
            }
*/
            //find points to create path with
            Point p1 = new Point(a.x - 3, a.y + 3);
            Point p2 = new Point(a.x + 3, a.y + 3);
            Point p3 = new Point(a.x + 3, a.y - 3);
            Point p4 = new Point(a.x - 3, a.y - 3);

            List<Point> points = new ArrayList<>(List.of(p1, p2, p3, p4));
            Point start = new Point(0, 0);
            points.sort(Comparator.comparingDouble(start::dist));
            Point pp1 = points.get(1);
            Point pp2 = points.get(2);

            var path1 = createPathThroughPoint(pp1, s);
            var path2 = createPathThroughPoint(pp2, s);

            if(path1.isValid()) {
                printPath(path1, writer);
            } else if(path2.isValid()) {
                printPath(path2, writer);
            } else {
                printPath(path2, writer);

                //writer.write("no valid path\n");
                //writer.write("no valid path\n");
            }

            writer.write("\n");
        }
    }

    private static TotalPath createPathThroughPoint(Point mid, Point end) {
        int xPace = getMaxBrakePace(mid.x, end.x);
        int yPace = getMaxBrakePace(mid.y, end.y);

        var xPath1 = createPath(0, mid.x, 0, xPace);
        var yPath1 = createPath(0, mid.y, 0, yPace);
        xPath1.addFirst(0);
        yPath1.addFirst(0);

        int diff = getTimeSum(xPath1) - getTimeSum(yPath1);
        if(diff < 0) {
            for(int d = 0; d < Math.abs(diff); d++) {
                xPath1.addFirst(0);
            }
        } else {
            for(int d = 0; d < Math.abs(diff); d++) {
                yPath1.addFirst(0);
            }
        }

        var xPath2 = createPath(mid.x, end.x, xPath1.getLast(), 0);
        var yPath2 = createPath(mid.y, end.y, yPath1.getLast(), 0);
        xPath2.addLast(0);
        yPath2.addLast(0);

        xPath1.addAll(xPath2);
        yPath1.addAll(yPath2);

        return new TotalPath(
                xPath1,
                yPath1
        );
    }

    private static List<Integer> createPath(int startPos, int endPos, int startPace, int endPace) {
        var paces = new ArrayList<Integer>();

        int dir = (int) Math.signum(endPos - startPos);
        int absPos = Math.abs(endPos - startPos);
        int pace = startPace;
        while(absPos > 0) {
            int brakePath = getBrakePath(pace, endPace);

            if(brakePath == absPos) {
                //nothing
            } else if(brakePath > absPos) {
                pace = decelerate(pace);
            } else {
                pace = accelerate(pace, dir);
            }
            paces.add(pace);

            absPos--;
        }

        return paces;
    }

    private static int getBrakePath(int pace, int endPace) {
        if(pace == 0) {
            return 0;
        }

        if(endPace == 0) {
            return 6 - Math.abs(pace);
        }

        return Math.abs(endPace) - Math.abs(pace);
    }

    private static int getMaxBrakePace(int a, int b) {
        int xDist = Math.abs(a - b);
        if(xDist == 0) {
            return 0;
        }

        return Math.max(1, 6 - xDist);
    }

    private static int getTimeSum(List<Integer> path) {
        return path.stream().mapToInt(Math::abs).sum();
    }

    private static List<Integer> createPath(int pos) {
        var paces = new ArrayList<Integer>();

        int absPos = Math.abs(pos);

        paces.add(0);
        int pace = 0;
        while(absPos > 0) {
            int brakePath = pace == 0 ? 0 : 5 - pace + 1;

            if(brakePath == absPos) {
                //nothing
            } else if(brakePath > absPos) {
                pace = decelerate(pace);
            } else {
                pace = accelerate(pace, 1);
            }
            paces.add(pace);

            absPos--;
        }
        paces.add(0);

        for(int i = 0;i < paces.size();i++) {
            if(pos < 0) {
                paces.set(i, paces.get(i) * -1);
            }
        }

        return paces;
    }

    private static int accelerate(int prevPace, int dir) {
        if(prevPace == 0) {
            return 5 * dir;
        } else if(Math.abs(prevPace) == 1) {
            return prevPace;
        }

        return prevPace - dir;
    }

    private static int decelerate(int prevPace) {
        if(prevPace == 0) {
            return 0;
        } else if(prevPace == 5) {
            return 0;
        }

        return prevPace + 1;
    }

    private static void printPath(List<Integer> path, FileWriter writer) throws IOException {
        for(var a : path) {
            writer.write(a + " ");
        }
        writer.write("\n");
    }

    private static void printPath(TotalPath path, FileWriter writer) throws IOException {
        printPath(path.x, writer);
        printPath(path.y, writer);
    }

    private static List<Integer> createNormalizedPath(List<Integer> path) {
        var normalized = new ArrayList<Integer>();

        for(var a : path) {
            var aAbs = Math.abs(a);

            for(int i = 0;i < aAbs;i++) {
                normalized.add(a);
            }
        }

        return normalized;
    }
}
