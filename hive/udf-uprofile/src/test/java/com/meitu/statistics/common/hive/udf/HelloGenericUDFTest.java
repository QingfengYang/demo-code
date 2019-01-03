package com.meitu.statistics.common.hive.udf;

import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaBooleanObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HelloGenericUDFTest {

    @Test
    public void testComplexUDFReturnsCorrectValues() throws HiveException {

        // 建立需要的模型
        HelloGenericUDF example = new HelloGenericUDF();
        ObjectInspector stringOI = PrimitiveObjectInspectorFactory.javaStringObjectInspector;
        ObjectInspector listOI = ObjectInspectorFactory.getStandardListObjectInspector(stringOI);
        JavaBooleanObjectInspector resultInspector = (JavaBooleanObjectInspector) example.initialize(new ObjectInspector[]{listOI, stringOI});

        // create the actual UDF arguments
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        // 测试结果

        // 存在的值
        Object result = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(list), new GenericUDF.DeferredJavaObject("a")});
        Assert.assertEquals(true, resultInspector.get(result));

        // 不存在的值
        Object result2 = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(list), new GenericUDF.DeferredJavaObject("d")});
        Assert.assertEquals(false, resultInspector.get(result2));

        // 为null的参数
        Object result3 = example.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(null), new GenericUDF.DeferredJavaObject(null)});
        Assert.assertNull(result3);
    }
}
