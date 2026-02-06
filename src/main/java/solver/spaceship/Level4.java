package solver.spaceship;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level4 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine();

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            String line = reader.nextLine();
            String[] split = line.split(" ");
            String[] splitPos = split[0].split(",");
            int x = Integer.parseInt(splitPos[0]);
            int y = Integer.parseInt(splitPos[1]);
            int limit = Integer.parseInt(split[1]);

            var xPath = createPath(x);
            var yPath = createPath(y);

            printPath(xPath, writer);
            printPath(yPath, writer);
            writer.write("\n");
        }
    }

    private static void printPath(List<Integer> path, FileWriter writer) throws IOException {
        for(var a : path) {
            writer.write(a + " ");
        }
        writer.write("\n");
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
                pace = accelerate(pace);
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

    private static int accelerate(int prevPace) {
        if(prevPace == 0) {
            return 5;
        } else if(prevPace == 1) {
            return 1;
        }

        return prevPace - 1;
    }

    private static int decelerate(int prevPace) {
        if(prevPace == 0) {
            return 0;
        } else if(prevPace == 5) {
            return 0;
        }

        return prevPace + 1;
    }
}
