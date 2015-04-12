import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;


public class TestUseAssertEqualsBugs {

    @Test
    public void test() {
        List<Integer> list = new ArrayList<>();
        assertEquals(2, list.size());
        assertEquals(2, list.size());
    }

    @Test
    public void testUseAssertEquals(String s, String s2) {
        Assert.assertEquals(s, s2);
        Assert.assertEquals(s.length(), s2.length());
    }

}
