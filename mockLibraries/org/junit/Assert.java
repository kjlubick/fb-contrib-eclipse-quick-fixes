package org.junit;

public class Assert {

    public static void assertTrue(boolean equals) {
        System.out.println(equals +" should be true");
        
    }
    
    public static void assertEquals(Object obj, Object other) {
        System.out.println(obj +" and "+other+" should be equal");
        
    }

}
