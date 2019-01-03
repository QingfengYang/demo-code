package com.meitu.statistics.common.hive.udf;

import org.junit.Assert;
import org.junit.Test;

public class HelloUDFTest {

    @Test
    public void testUDF() {
        HelloUDF helloUDF = new HelloUDF();
        Assert.assertEquals("hello , world", helloUDF.evaluate("world"));
    }
}
