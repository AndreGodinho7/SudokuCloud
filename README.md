The following may be performed to run the compiled server class (provided by the faculty).
This launches the example server listening on port 8000:
* cd $HOME/cnv-project
* java pt.ulisboa.tecnico.cnv.server.WebServer

To recompile the server class:
* cd $HOME/cnv-project
* javac pt/ulisboa/tecnico/cnv/server/WebServer.java


To recompile the solver classes:
* cd $HOME/cnv-project
* javac pt/ulisboa/tecnico/cnv/solver/*.java

To run the solvers directly with the command line:
* cd $HOME/cnv-project
* java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -n1 9 -n2 9 -un 81 -i SUDOKU_PUZZLE_9x19_101 -s DLX -b
[[2,0,0,8,0,5,0,9,1],[9,0,8,0,7,1,2,0,6],[0,1,4,2,0,3,7,5,8],[5,0,1,0,8,7,9,2,4],[0,4,9,6,0,2,0,8,7],[7,0,2,1,4,9,3,0,5],[1,3,7,5,0,6,0,4,9],[4,2,5,0,1,8,6,0,3],[0,9,6,7,3,4,0,1,2]]
* java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -n1 16 -n2 16 -un 128 -i SUDOKU_PUZZLE_16x16_02 -s BFS -b
[[11,0,0,12,0,14,0,2,7,16,0,6,0,5,1,3],[0,16,0,6,10,12,0,1,2,11,14,5,0,13,0,15],[1,4,3,14,0,7,5,9,0,10,0,15,2,11,0,12],[7,5,2,13,0,16,15,4,3,9,0,12,0,14,6,8],[2,15,0,1,8,13,0,3,0,14,9,7,16,10,0,5],[13,12,0,10,9,11,4,7,0,5,15,3,14,8,2,6],[0,8,14,5,0,1,0,16,10,13,0,11,0,9,12,7],[0,9,7,11,14,5,2,10,12,6,16,8,15,1,0,13],[15,1,11,2,4,6,16,13,5,8,7,10,12,3,14,9],[12,7,5,4,2,9,1,15,14,3,11,13,8,6,10,16],[14,6,9,3,7,8,10,5,16,2,12,1,13,4,15,11],[16,13,10,8,12,3,14,11,15,4,6,9,7,2,5,1],[6,2,1,16,3,4,7,8,13,15,5,14,11,12,9,10],[5,3,4,15,16,10,9,6,11,12,8,2,1,7,13,14],[10,14,13,7,5,2,11,12,9,1,3,16,6,15,8,4],[8,11,12,9,1,15,13,14,6,7,10,4,5,16,3,2]]
*java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -n1 25 -n2 25 -un 450 -i SUDOKU_PUZZLE_25x25_01 -s CP -b
[[24,0,0,9,0,21,0,19,4,10,0,20,0,1,16,7,0,18,0,13,5,15,0,8,6],[14,3,8,0,12,0,24,15,11,2,21,0,19,13,9,0,20,0,1,6,10,0,23,16,25],[16,7,0,11,4,13,25,1,0,18,0,22,15,6,5,9,0,17,10,23,0,12,0,21,24],[22,5,1,0,19,16,6,0,23,7,4,24,14,0,25,15,21,12,8,11,17,0,20,18,3],[0,6,0,10,17,20,0,3,0,5,11,18,0,8,23,16,24,4,19,25,1,13,22,7,9],[20,0,3,15,5,0,16,23,21,13,1,0,22,0,7,18,6,19,14,10,24,11,4,0,17],[0,17,9,21,24,10,0,14,1,3,19,13,0,11,18,2,0,23,22,12,25,7,0,15,5],[13,14,25,0,11,0,18,17,7,19,10,12,9,4,21,0,3,0,15,8,22,0,2,0,16],[12,16,23,8,1,25,5,22,15,11,0,17,3,24,20,13,4,9,21,7,6,10,0,19,14],[7,0,22,0,10,2,8,0,6,12,23,5,16,0,14,0,25,24,20,17,13,21,3,9,1],[0,20,5,23,2,14,0,10,13,4,16,6,0,19,17,3,8,7,0,15,0,24,25,22,18],[3,0,6,1,8,0,15,0,20,23,24,2,10,18,13,14,9,0,11,22,19,17,16,4,7],[10,24,17,16,18,7,0,25,19,8,0,3,0,12,11,6,0,20,4,2,23,14,13,1,15],[21,19,4,22,15,0,11,16,2,24,9,0,7,14,8,23,10,13,17,1,20,0,6,0,12],[11,13,0,14,7,22,3,6,0,1,15,23,20,5,4,19,0,25,16,24,9,8,0,2,21],[5,11,10,0,21,6,19,0,22,17,13,4,12,0,2,8,15,14,23,16,7,0,1,25,20],[0,9,18,20,14,1,10,8,0,21,17,19,11,7,24,4,22,3,0,5,0,6,15,23,13],[17,15,7,12,3,0,23,0,9,20,5,16,25,0,6,10,11,0,13,18,21,19,24,0,8],[19,1,13,4,23,3,12,24,14,25,8,10,18,9,15,20,17,6,7,21,2,16,5,11,22],[6,8,16,25,22,11,13,7,5,15,14,21,23,20,1,24,12,2,9,19,3,18,17,10,4],[18,22,11,3,16,5,2,12,10,9,25,14,8,21,19,17,13,15,6,20,4,1,7,24,23],[23,12,15,7,13,8,17,20,25,6,18,11,4,2,22,21,1,10,24,9,16,3,14,5,19],[1,4,24,5,20,15,7,11,3,16,6,9,13,23,12,25,19,8,2,14,18,22,21,17,10],[25,10,14,17,6,19,1,21,24,22,7,15,5,16,3,12,23,11,18,4,8,20,9,13,2],[8,21,19,2,9,23,4,13,18,14,20,1,24,17,10,22,7,16,5,3,15,25,12,6,11]]
