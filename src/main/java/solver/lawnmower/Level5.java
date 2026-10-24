package solver.lawnmower;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Level5 {
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        //same task as level 4, the path finder does not care where the tree is
        Level4.solve(reader, writer);
    }
}
