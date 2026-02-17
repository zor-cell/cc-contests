package solver;

import config.Config;
import config.ConfigLoader;
import io.IOManager;
import io.IOUtils;
import log.CustomLogger;
import solver.spaceship.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Solver {
    private static Logger log;

    /**
     * Do not change this method. Method Loads the config and initializes the IOManager.
     */
    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.xml");

        CustomLogger.createLogger(config);
        log = CustomLogger.getLogger();

        IOManager ioManager = new IOManager(config);
        ioManager.initialize();
        ioManager.execute();
    }

    /**
     * The verification method to read content from a given {@code input} to verify whether a solution from a given {@code output} was correct.
     * This method is only applied if a visualizer is provided, which can verify correctness.
     * DO NOT CHANGE PARAMETERS OR RETURN TYPE
     */
    public static void verify(Scanner input, Scanner output, SolutionVerifier verifier) throws IOException {
        int maxRuns = input.nextInt();
        input.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            String inputLine1 = input.nextLine();
            String inputLine2 = input.nextLine();

            String xPath = output.nextLine();
            String yPath = output.nextLine();
            output.nextLine();

            String inputLine = inputLine1 + "\n" + inputLine2;
            String paths = xPath + "\n" + yPath;

            verifier.checkValidity(run, inputLine, paths);
        }
    }


    /**
     * The solving method to read from an input file via {@code reader} and write the solution to an output file via {@code writer}.
     * This method is applied to all input files.
     * DO NOT CHANGE PARAMETERS OR RETURN TYPE
     */
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        Level5.solve(reader, writer);
    }
}
