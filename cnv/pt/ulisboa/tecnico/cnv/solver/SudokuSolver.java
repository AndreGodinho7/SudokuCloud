package pt.ulisboa.tecnico.cnv.solver;

public interface SudokuSolver {
    boolean runSolver(Solver solver);

    void setPuzzle(int[][] puzzle);

    int[][] getPuzzle();
    int[][] getSolution();

    void printPuzzle();
    void printSolution();

    @Override
    String toString();
}
