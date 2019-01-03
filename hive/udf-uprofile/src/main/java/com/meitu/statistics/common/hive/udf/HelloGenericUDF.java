package com.meitu.statistics.common.hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;

import java.util.List;


@Description(name = "list_contains",
        value = "list_contains(list, s) - 输入list和string,判断list中是否包含该string",
        extended = "Example:\n " + "  > SELECT list_contains(list, s);\n" + " true")
public class HelloGenericUDF extends GenericUDF {

    ListObjectInspector listOI;
    StringObjectInspector elementOI;

    @Override
    public String getDisplayString(String[] arg0) {
        return "HelloGenericUDF()";
    }

    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        if (arguments.length != 2) {
            throw new UDFArgumentLengthException("arrayContainsExample only takes 2 arguments: List<T>, T");
        }
        // 1. 检查是否接收到正确的参数类型
        ObjectInspector a = arguments[0];
        ObjectInspector b = arguments[1];
        if (!(a instanceof ListObjectInspector) || !(b instanceof StringObjectInspector)) {
            throw new UDFArgumentException("first argument must be a list / array, second argument must be a string");
        }
        this.listOI = (ListObjectInspector) a;
        this.elementOI = (StringObjectInspector) b;

        // 2. 检查list是否包含的元素都是string
        if (!(listOI.getListElementObjectInspector() instanceof StringObjectInspector)) {
            throw new UDFArgumentException("first argument must be a list of strings");
        }

        // 返回类型是boolean，所以我们提供了正确的object inspector
        return PrimitiveObjectInspectorFactory.javaBooleanObjectInspector;
    }

    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {

        // 利用object inspectors从传递的对象中得到list与string
        List<String> list = (List<String>) this.listOI.getList(arguments[0].get());
        String arg = elementOI.getPrimitiveJavaObject(arguments[1].get());

        // 检查空值
        if (list == null || arg == null) {
            return null;
        }

        // 判断是否list中包含目标值
        for (String s : list) {
            if (arg.equals(s)) {
                return new Boolean(true);
            }
        }
        return new Boolean(false);
    }

}
