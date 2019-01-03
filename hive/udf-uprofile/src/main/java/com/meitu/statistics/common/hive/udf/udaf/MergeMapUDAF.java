package com.meitu.statistics.common.hive.udf.udaf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFParameterInfo;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFResolver2;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;

import java.util.HashMap;
import java.util.Map;

@Description(name = "merge_map", value = "_FUNC_(x) - Return a merged map")
public class MergeMapUDAF implements GenericUDAFResolver2 {

    public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo paramInfo) throws SemanticException {
        ObjectInspector[] parameters = paramInfo.getParameterObjectInspectors();
        if (parameters.length != 2) {
            throw new UDFArgumentException("2 Argument expected");
        } else {
            return new MapCollectUDAFEvaluator();
        }
    }

    public GenericUDAFEvaluator getEvaluator(TypeInfo[] parameters) throws SemanticException {
        if (parameters.length != 2) {
            throw new UDFArgumentTypeException(parameters.length - 1,
                    "One argument is expected to return an Array.");
        }
        return new MapCollectUDAFEvaluator();
    }

    public static class MapCollectUDAFEvaluator extends GenericUDAFEvaluator {
        // For PARTIAL1 and COMPLETE: ObjectInspectors for original data
        private ObjectInspector inputOI;
        // For PARTIAL2 and FINAL: ObjectInspectors for partial aggregations (Map of objects)
        private StandardMapObjectInspector internalMergeOI;

        public ObjectInspector init(Mode m, ObjectInspector[] parameters) throws HiveException {
            super.init(m, parameters);
            System.out.println("ObjectInspector[] size: " + parameters.length);
            // init output object inspectors
            // The output of a partial aggregations is Map
            if (m == Mode.PARTIAL1) {
                inputOI = parameters[0];
                return ObjectInspectorFactory.getStandardMapObjectInspector(
                        PrimitiveObjectInspectorFactory.getPrimitiveWritableObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.STRING),
                        ObjectInspectorUtils.getStandardObjectInspector(inputOI));
            } else {
                if (!(parameters[0] instanceof StandardMapObjectInspector)) {
                    inputOI = ObjectInspectorUtils.getStandardObjectInspector(parameters[0]);
                    return (StandardMapObjectInspector) ObjectInspectorFactory.getStandardMapObjectInspector(
                            PrimitiveObjectInspectorFactory.getPrimitiveWritableObjectInspector(PrimitiveObjectInspector.PrimitiveCategory.STRING),
                            inputOI);
                } else {
                    internalMergeOI = (StandardMapObjectInspector) parameters[0];
                    inputOI = internalMergeOI.getMapValueObjectInspector();
                    return (StandardMapObjectInspector) ObjectInspectorUtils.getStandardObjectInspector(internalMergeOI);
                }
            }
        }

        static class MapAggBuffer extends AbstractAggregationBuffer {
            Map<Object, Object> collectMap = new HashMap<Object, Object>();

            public int estimate() {
                return (32 + 2) * collectMap.size();
            }
        }

        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            MapAggBuffer mapAggBuffer = new MapAggBuffer();
            reset(mapAggBuffer);
            return mapAggBuffer;
        }

        public void reset(AggregationBuffer agg) throws HiveException {
            MapAggBuffer mapAggBuffer = (MapAggBuffer)agg;
            mapAggBuffer.collectMap = new HashMap<Object, Object>();
        }

        public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
            Object k = parameters[0];
            Object v = parameters[1];
            if (k == null || v == null) {
                throw new HiveException("Key or value is null. k = " + k + ", v = " + v);
            }

            MapAggBuffer mapAgg = (MapAggBuffer) agg;
            putInToSet(k, v, mapAgg);
        }

        public Object terminatePartial(AggregationBuffer agg) throws HiveException {
            MapAggBuffer myAgg = (MapAggBuffer)agg;
            return myAgg.collectMap;
        }

        public void merge(AggregationBuffer agg, Object partial) throws HiveException {
            MapAggBuffer leftAgg = (MapAggBuffer)agg;
            Map<Object, Object> partialResult = (Map<Object, Object>) internalMergeOI.getMap(partial);

            for (Map.Entry<Object, Object> entry : partialResult.entrySet()) {
                putInToSet(entry.getKey(), entry.getValue(), leftAgg);
            }
        }

        public Object terminate(AggregationBuffer agg) throws HiveException {
            MapAggBuffer myAgg = (MapAggBuffer)agg;
            return myAgg.collectMap;
        }

        private void putInToSet(Object key, Object val, MapAggBuffer mapAgg) {
            mapAgg.collectMap.put(key, val);
        }
    }
}
