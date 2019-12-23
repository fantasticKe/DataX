### Datax HttpWriter
#### 1 快速介绍

HttpWriter 插件利用HttpClient连接池技术，以post请求的方式将数据发往制定的第三方数据接口。

#### 2 实现原理

HttpWriter通过Datax框架获取Reader生成的数据，然后将Datax支持的类型通过逐一判断转换成用户制定的输出类型。

#### 3 功能说明
* 该示例从mysql读一份数据到指定的API接口。

		```json
   {
       "job": {
           "content": [{
               "reader": {
                   "parameter": {
                       "username": "root",
                       "password": "root",
                       "connection": [{
                           "querySql": ["select * from test"],
                           "jdbcUrl": ["jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8"]
                       }]
                   },
                   "name": "mysqlreader"
               },
               "writer": {
                   "parameter": {
                       "targetUrl": "http://localhost:8080/test",
                       "method": "POST",
                       "param": "params",
                       "limit": 1000,
                       "column": [
                         {
                           "index": 0,
                           "name": "keyword",
                           "type": "string"
                         },
                         {
                           "index":1,
                           "name":"name",
                           "type":"json"
                         },
                         {
                           "index":2,
                           "name":"corp_info",
                           "type":"string"
                         }
                         {
                           "index":3,
                           "name":"source",
                           "type":"string"
                         },
                         {
                           "index":4,
                           "name":"type",
                           "type":"string"
                         },
                         {
                           "index":5,
                           "name":"update_timestamp",
                           "type":"date"
                         },
                         {
                           "index":6,
                           "name":"index",
                           "type":"int"
                         }
                       ],
                       "httpConfig": {
                         "retryCount": 3,
                         "maxTotal": 200,
                         "defaultMaxPerRoute": 200,
                         "connectionRequestTimeout": 2000,
                         "connectTimeout": 1000,
                         "socketTimeout": 10000,
                         "proxy": ""
                       }
                     },
                   "name": "httpwriter"
               }
           }],
           "setting": {
               "errorLimit": {
                   "record": 0,
                   "percentage": 0.20
               },
               "speed": {
                   "channel": 4
               }
           }
       }
   }
   ```
   
   

#### 4 参数说明

* targetUrl: 数据API地址【必填】
* param： 表单参数字段。【必填】
* limit： 每秒请求限制数。【选填】
* column：输出JSON数据列名。【必填】
* index：数据列下标。【必填】
* name：数据列的名字。【必填】
* type：数据列的类型。【必填】
* httpConfig：http连接池配置。【选填】
* retryCount：请求失败重试次数。【选填】
* maxTotal：最大连接数。【选填】
* defaultMaxPerRoute：每个路由基础的最大连接数。【选填】
* connectionRequestTimeout：从连接管理器请求连接时候的超时。【选填】
* connectTimeout：连接超时。【选填】
* socketTimeout：读取数据超时。【选填】
* proxy：代理服务地址。【选填】


#### 5 性能报告
#### 6 测试报告