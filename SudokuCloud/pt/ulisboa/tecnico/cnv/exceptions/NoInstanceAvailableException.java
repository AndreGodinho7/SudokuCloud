package pt.ulisboa.tecnico.cnv.exceptions;

public class NoInstanceAvailableException extends Exception {
    public NoInstanceAvailableException() {
        System.out.println("There are no healthy instances available.");
    }
}
