package appserver.job.impl;

/**
 * The class [FibonacciAux] is a helper class for calculating the Fibonacci number of a given input.
 * 
 * @author sampath
 */
public class FibonacciAux {
    
    Integer number = null;
    
    /**
     * Constructor for creating a FibonacciAux object with the given input number.
     * 
     * @param number the input number for which the Fibonacci number is to be calculated
     */
    public FibonacciAux(Integer number) {
        this.number = number;
    }
    
    /**
     * Method for calculating the Fibonacci number of the input number.
     * 
     * @return the Fibonacci number of the input number
     */
    public Integer getResult() {
        return fibonacci(number);
    }
    
    /**
     * Recursive method for calculating the Fibonacci number of a given input number.
     * 
     * @param n the input number for which the Fibonacci number is to be calculated
     * 
     * @return the Fibonacci number of the input number
     */
    private int fibonacci(Integer n) {
        switch (n) {
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return fibonacci(n-1) + fibonacci(n-2);
        }
    }
}
