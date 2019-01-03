package com.meitu.statistics.common.hive.udf;


import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDF;


@Description(name = "say_hello",
        value = "say_hello(param) - 根据输入的name,返回'hello , ' + name",
        extended = "Example:\n " + "  > SELECT say_hello('meitu');\n" + "  hello , meitu")
public class HelloUDF extends UDF {

    public String evaluate(String name) {
        return name == null ? name : "hello , " + name;
    }
}
