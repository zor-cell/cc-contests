package solver.nanofabricator;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level1 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine();

        for(int run = 0;run < maxRuns;run++) {
            int radius = Integer.parseInt(reader.nextLine().trim());

            writer.write(renderBoard(radius));
            writer.write("\n");
        }
    }

    /**
     * Renders a hexagonal board of the given radius: 2r+1 rows, row i has 2r+1-|i-r| cells,
     * border cells are '*', inner cells 'O', separated and indented by '-'.
     */
    public static String renderBoard(int radius) {
        StringBuilder sb = new StringBuilder();

        for(int row = 0;row <= 2 * radius;row++) {
            int indent = Math.abs(row - radius);
            int cells = 2 * radius + 1 - indent;
            boolean borderRow = row == 0 || row == 2 * radius;

            sb.append("-".repeat(indent));
            for(int c = 0;c < cells;c++) {
                if(c > 0) sb.append('-');
                boolean border = borderRow || c == 0 || c == cells - 1;
                sb.append(border ? '*' : 'O');
            }
            sb.append("-".repeat(indent));
            sb.append("\n");
        }

        return sb.toString();
    }
}
