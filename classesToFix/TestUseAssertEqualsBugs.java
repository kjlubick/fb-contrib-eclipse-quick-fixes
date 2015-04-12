import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;


public class TestUseAssertEqualsBugs {

    @Test
    public void test() {
        List<Integer> list = new ArrayList<>();
        assertTrue(list.size() == 2);
        assertTrue(2 == list.size());
    }

    @Test
    public void testUseAssertEquals(String s, String s2) {
        Assert.assertTrue(s.equals(s2));
        Assert.assertTrue(s.length() == s2.length());
    }

}
