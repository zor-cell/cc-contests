package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level1 {
    private static final Logger log = CustomLogger.getLogger();

    private static final int DESK_LENGTH = 3;

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            long x = reader.nextLong();
            long y = reader.nextLong();

            //x is divisible by the desk length, so the room can be filled completely
            long desks = x * y / DESK_LENGTH;

            //write result to file
            writer.write(desks + "\n");
        }
    }
}
