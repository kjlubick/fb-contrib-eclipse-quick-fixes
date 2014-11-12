import java.math.BigDecimal;


public class BigDecimalStringBugs {

    public BigDecimal makeDecimal() {
        return new BigDecimal(1.23456);
    }
    
    public BigDecimal makeDecimal2() {
        return new BigDecimal(1.23456);
    }
    
}
