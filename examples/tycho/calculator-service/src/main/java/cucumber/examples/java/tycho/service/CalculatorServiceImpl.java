package cucumber.examples.java.tycho.service;

import cucumber.examples.java.tycho.CalculatorService;

public class CalculatorServiceImpl implements CalculatorService {

    @Override
    public int add(int a, int b) {
        return a + b;
    }

}
