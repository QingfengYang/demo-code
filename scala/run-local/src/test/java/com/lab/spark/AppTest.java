package com.lab.spark;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


 /* Unit test for simple App. */

public class AppTest 
{

    @Test
    public void shouldAnswerWithTrue()
    {
        //assertTrue( true );
        String[] arr = new String[]{"a", "b", "c", "d"};
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                System.out.printf("%s-%s\n", arr[i], arr[j]);
            }
        }
    }
}
