package com.meitu.statistics.common.hive.udf;

import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.JavaStringObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeoHashUDFTest {

    @Test
    public void testGeoHash() throws HiveException {
        GeoHashUDF geoHashUDF = new GeoHashUDF();
        ObjectInspector doubleOI1 = PrimitiveObjectInspectorFactory.javaDoubleObjectInspector;
        ObjectInspector doubleOI2 = PrimitiveObjectInspectorFactory.javaDoubleObjectInspector;
        ObjectInspector intOI = PrimitiveObjectInspectorFactory.javaIntObjectInspector;
        JavaStringObjectInspector resultInspector = (JavaStringObjectInspector) geoHashUDF.initialize(new ObjectInspector[]{doubleOI1, doubleOI2, intOI});

        //22.5411130000,113.9534040000,12
        double latitude = 22.5411130000;
        double longitude = 113.9534040000;
        int desiredPrecision = 12;

        Object result = geoHashUDF.evaluate(new GenericUDF.DeferredObject[]{new GenericUDF.DeferredJavaObject(latitude),
                new GenericUDF.DeferredJavaObject(longitude),
                new GenericUDF.DeferredJavaObject(desiredPrecision)
        });
        String geoHash = resultInspector.getPrimitiveJavaObject(result);
        assertEquals("ws101p6rdb08", geoHash);
    }
}
