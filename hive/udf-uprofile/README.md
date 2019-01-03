#### 项目业务与实现介绍

* 项目名：udf-demo
* 业务：
    demo项目，实现udf函数，用于hql语句调用
* 实现：
    继承Hive的GenericUDF或者UDF类，重写相应方法
    
#### 编码规范

* 在包src/main/java/com.meitu.statistics.common.hive.udf下新建java类
* 在包test/java/com.meitu.statistics.common.hive.udf下新建对应的单元测试类，编写相关单元测试，尽可能测试完整
* 编写完后，在"数据工坊-数据管理-udf管理-创建udf"申请创建，并上传相关代码

#### 注意事项

* pom中相关版本配置无须更改，已参考线上版本做相应配置
* 编写完代码后，在数据工坊上传源代码文件即可，无须更新维护gitlab
* 目前只支持java udf，其他语言暂不支持
    