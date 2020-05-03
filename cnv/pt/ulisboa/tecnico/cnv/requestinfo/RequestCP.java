package pt.ulisboa.tecnico.cnv.requestinfo;


import java.util.ArrayList;
import java.util.List;

public class RequestCP extends Request {

    double CP_constant = (long) 1.8;
    double miss_ele_constant = 2;

    public RequestCP(String puzzle_name, String strategy, int sizeX, int sizeY, int miss_ele) {
        super(puzzle_name, strategy, sizeX, sizeY, miss_ele);
    }

    //Get all prime numbers in the interval
    private int getPrimes(int min, int max) {
        boolean [] primes = new boolean[max];
        for(int i = min; i < max; i++) {
            primes[i] = true;
        }
        float limit = (float) Math.sqrt(max);
        for(float i = min; i < limit; i++) {
            if(primes[(int)i]) {
                for(float j = i * i; j < max; j += i) {
                    primes[(int)j] = false;
                }
            }
        }
        int sum = 0;
        for(boolean b : primes) {
            sum += b ? 1 : 0;
        }
        return sum;
    }

    @Override
    public double calculateCost(long methods) {
        int primes = getPrimes(2, miss_ele);
        double missing_ele = ((double) primes)/(sizeX*sizeY);
        return CP_constant*(methods + methods*miss_ele_constant*missing_ele);
    }
}
