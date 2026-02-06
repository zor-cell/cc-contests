package solver.spaceship;

import io.IOUtils;
import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level2 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine();

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            List<Integer> paces = IOUtils.readList(reader, " ", Integer::parseInt);

            int space = 0;
            int time = 0;
            for(int p : paces) {
                space += paceToSpace(p);
                time += paceToTime(p);
            }

            writer.write(space + " " + time + "\n");
        }
    }

    private static int paceToSpace(int pace) {
        return (int) Math.signum(pace);
    }

    private static int paceToTime(int pace) {
        if(pace == 0) return 1;

        return Math.abs(pace);
    }
}
