package com.meitu.statistics.common.hive.udaf;

import com.meitu.statistics.common.hive.udf.udaf.CollectMapUDAF;
import com.meitu.statistics.common.hive.udf.udaf.CollectMapUDAF.*;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CollectMapUDAFTest {

    @Test
    public void testCollectMap1() throws HiveException {
        String[] keys = new String[]{"A", "B", "C"};
        Long[] values = new Long[] {1L, 20L, 30L};

        MapCollectUDAFEvaluator collectUDAFEvaluator = new CollectMapUDAF.MapCollectUDAFEvaluator();
        ObjectInspector[] params = new ObjectInspector[]{
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.STRING),
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.LONG)
        };
        collectUDAFEvaluator.init(GenericUDAFEvaluator.Mode.PARTIAL1, params);

        GenericUDAFEvaluator.AggregationBuffer aggBuff = collectUDAFEvaluator.getNewAggregationBuffer();
        for (int i = 0; i < keys.length; i++) {
            Object[] keyValPair = new Object[]{ keys[i], values[i]};
            collectUDAFEvaluator.iterate(aggBuff, keyValPair);
        }
        Map<Object, Object> collectMap = (Map<Object, Object>) collectUDAFEvaluator.terminatePartial(aggBuff);
        boolean result = true;
        for (int i = 0; i < keys.length; i++) {
            Long val = Long.parseLong(collectMap.get(keys[i]).toString());
            result = result && (val == values[i]);
        }
        Assert.assertTrue(result);
    }


    @Test
    public void testCollectMap2() throws HiveException {
        // partition 1
        String[] keys1 = new String[]{"A", "B", "C"};
        Long[] values1 = new Long[] {1L, 2L, 3L};

        MapCollectUDAFEvaluator collectUDAFEvaluator1 = new CollectMapUDAF.MapCollectUDAFEvaluator();
        ObjectInspector[] params1 = new ObjectInspector[]{
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.STRING),
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.LONG)
        };
        collectUDAFEvaluator1.init(GenericUDAFEvaluator.Mode.PARTIAL2, params1);
        GenericUDAFEvaluator.AggregationBuffer aggBuff1 = collectUDAFEvaluator1.getNewAggregationBuffer();
        for (int i = 0; i < keys1.length; i++) {
            Object[] keyValPair = new Object[]{ keys1[i], values1[i]};
            collectUDAFEvaluator1.iterate(aggBuff1, keyValPair);
        }


        // partitions 2
        String[] keys2 = new String[]{"D", "E", "H"};
        Long[] values2 = new Long[] {4L, 5L, 6L};

        MapCollectUDAFEvaluator collectUDAFEvaluator2 = new CollectMapUDAF.MapCollectUDAFEvaluator();
        ObjectInspector[] params2 = new ObjectInspector[]{
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.STRING),
                PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.LONG)
        };
        collectUDAFEvaluator2.init(GenericUDAFEvaluator.Mode.PARTIAL2, params2);
        GenericUDAFEvaluator.AggregationBuffer aggBuff2 = collectUDAFEvaluator1.getNewAggregationBuffer();
        for (int i = 0; i < keys1.length; i++) {
            Object[] keyValPair = new Object[]{ keys2[i], values2[i]};
            collectUDAFEvaluator2.iterate(aggBuff2, keyValPair);
        }
        Object seMap = collectUDAFEvaluator2.terminatePartial(aggBuff2);

        // merge
        MapCollectUDAFEvaluator collectUDAFEvaluatorMerge = new CollectMapUDAF.MapCollectUDAFEvaluator();
        ObjectInspector[] paramsMerge = new ObjectInspector[] {
                ObjectInspectorFactory.getStandardMapObjectInspector(
                        PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.STRING),
                        PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.LONG)
                )
        };
        collectUDAFEvaluatorMerge.init(GenericUDAFEvaluator.Mode.PARTIAL2, paramsMerge);
        collectUDAFEvaluatorMerge.merge(aggBuff1, seMap);
        Map<Object, Object> collectMap = (Map<Object, Object>) collectUDAFEvaluatorMerge.terminate(aggBuff1);
        Assert.assertTrue(collectMap.size() == 6);
        boolean result = true;
        for (int i = 1; i < keys1.length; i++) {
            Long val = Long.parseLong(collectMap.get(keys1[i]).toString());
            result = result && (val == values1[i]);
        }

        for (int i = 1; i < keys2.length; i++) {
            Long val = Long.parseLong(collectMap.get(keys2[i]).toString());
            result = result && (val == values2[i]);
        }
        Assert.assertTrue(result);
    }
}
