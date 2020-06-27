package pt.ulisboa.tecnico.cnv.exceptions;

public class ServerFullLoadedException extends  Exception{
    public ServerFullLoadedException() {
        System.out.println("MESSAGE: Server is full loaded, requests have been paused.");
    }
}
