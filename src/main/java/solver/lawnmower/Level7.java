package solver.lawnmower;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Level7 {
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        //same task as level 6, the path finder does not care how many trees there are
        Level4.solve(reader, writer, true);
    }
}
