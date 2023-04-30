package appserver.job.impl;

import appserver.job.Tool;

/**
 * The class [Fibonacci] implements the Tool interface to calculate the nth Fibonacci number.
 * 
 * It uses the FibonacciAux class to perform the calculation.
 * @author Srinivasa
 */
public class Fibonacci implements Tool {
    
    FibonacciAux helper = null;
    
    // Override the go method from the Tool interface to calculate the Fibonacci number
    @Override
    public Object go(Object parameters) {
        
        // Create a new FibonacciAux object with the parameter passed in
        helper = new FibonacciAux((Integer) parameters);
        
        // Return the result of the Fibonacci calculation
        return helper.getResult();
    }
}
