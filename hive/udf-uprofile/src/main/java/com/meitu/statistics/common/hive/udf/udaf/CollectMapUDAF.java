package com.meitu.statistics.common.hive.udf.udaf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;

import java.util.HashMap;

@Description(name = "merge_map", value = "_FUNC_(x) - Return a merged map")
public class CollectMapUDAF extends AbstractGenericUDAFResolver {
    static final Log LOG = LogFactory.getLog(CollectMapUDAF.class.getName());

    public GenericUDAFEvaluator getEvaluator(TypeInfo[] parameters) throws SemanticException {
        if (parameters.length != 2) {
            throw new UDFArgumentTypeException(parameters.length - 1,
                    "One argument is expected to return an Array.");
        }
        return new MapCollectUDAFEvaluator();
    }

    public static class MapCollectUDAFEvaluator extends GenericUDAFEvaluator {
        // For PARTIAL1 and COMPLETE: ObjectInspectors for original data
        private PrimitiveObjectInspector inputKeyOI;
        private ObjectInspector inputValOI;
        // For PARTIAL2 and FINAL: ObjectInspectors for partial aggregations (list
        // of objs)
        private StandardMapObjectInspector moi;
        private StandardMapObjectInspector internalMergeOI;


        static class MapAggBuffer implements AggregationBuffer {
            HashMap<Object, Object> collectMap = new HashMap<Object, Object>();
        }

        public ObjectInspector init(Mode m, ObjectInspector[] parameters)
                throws HiveException {
            super.init(m, parameters);
            // init output object inspectors
            // The output of a partial aggregation is a list
            if (m == Mode.PARTIAL1) {
                inputKeyOI = (PrimitiveObjectInspector) parameters[0];
                inputValOI = parameters[1];

                return ObjectInspectorFactory.getStandardMapObjectInspector(
                        ObjectInspectorUtils.getStandardObjectInspector(inputKeyOI),
                        ObjectInspectorUtils.getStandardObjectInspector(inputValOI));
            } else {
                if (!(parameters[0] instanceof StandardMapObjectInspector)) {
                    inputKeyOI = (PrimitiveObjectInspector) ObjectInspectorUtils
                            .getStandardObjectInspector(parameters[0]);
                    inputValOI = ObjectInspectorUtils
                            .getStandardObjectInspector(parameters[1]);
                    return (StandardMapObjectInspector) ObjectInspectorFactory
                            .getStandardMapObjectInspector(inputKeyOI, inputValOI);
                } else {
                    internalMergeOI = (StandardMapObjectInspector) parameters[0];
                    inputKeyOI = (PrimitiveObjectInspector) internalMergeOI.getMapKeyObjectInspector();
                    inputValOI = internalMergeOI.getMapValueObjectInspector();
                    moi = (StandardMapObjectInspector) ObjectInspectorUtils.getStandardObjectInspector(internalMergeOI);
                    return moi;
                }
            }
        }

        @Override
        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            AggregationBuffer buff = new MapAggBuffer();
            reset(buff);
            return buff;
        }

        @Override
        public void iterate(AggregationBuffer agg, Object[] parameters)
                throws HiveException {
            Object k = parameters[0];
            Object v = parameters[1];

            if (k != null) {
                MapAggBuffer myagg = (MapAggBuffer) agg;
                putIntoSet(k, v, myagg);
            }
        }

        @Override
        public void merge(AggregationBuffer agg, Object partial)
                throws HiveException {
            MapAggBuffer myagg = (MapAggBuffer) agg;
            HashMap<Object, Object> partialResult = (HashMap<Object, Object>) internalMergeOI.getMap(partial);
            for (Object i : partialResult.keySet()) {
                putIntoSet(i, partialResult.get(i), myagg);
            }
        }

        @Override
        public void reset(AggregationBuffer buff) throws HiveException {
            MapAggBuffer arrayBuff = (MapAggBuffer) buff;
            arrayBuff.collectMap = new HashMap<Object, Object>();
        }

        @Override
        public Object terminate(AggregationBuffer agg) throws HiveException {
            MapAggBuffer myagg = (MapAggBuffer) agg;
            HashMap<Object, Object> ret = new HashMap<Object, Object>(myagg.collectMap);
            return ret;

        }

        private void putIntoSet(Object key, Object val, MapAggBuffer myagg) {
            Object keyCopy = ObjectInspectorUtils.copyToStandardObject(key, this.inputKeyOI);
            Object valCopy = ObjectInspectorUtils.copyToStandardObject(val, this.inputValOI);

            myagg.collectMap.put(keyCopy, valCopy);
        }

        @Override
        public Object terminatePartial(AggregationBuffer agg) throws HiveException {
            MapAggBuffer myagg = (MapAggBuffer) agg;
            HashMap<Object, Object> ret = new HashMap<Object, Object>(myagg.collectMap);
            return ret;
        }
    }
}
