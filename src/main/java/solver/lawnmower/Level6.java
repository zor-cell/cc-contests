package solver.lawnmower;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Level6 {
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        //same task as level 4, but the path has to end next to its start cell
        Level4.solve(reader, writer, true);
    }
}
