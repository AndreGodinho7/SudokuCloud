package pt.ulisboa.tecnico.cnv.solver;

public class SolverMain {

    public static void main(final String[] args) {


        // Get user-provided flags.
        final pt.ulisboa.tecnico.cnv.solver.SolverArgumentParser ap = new SolverArgumentParser(args);

        // Create solver instance from factory.
        final Solver s = SolverFactory.getInstance().makeSolver(ap);

        s.solveSudoku();
    }
}
