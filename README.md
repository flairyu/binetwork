# binetwork

用于测试二进制协议的网络通讯客户端。

# 编译
javac Main.java

# 运行
java main.class [server ip or name] [port] [byteorder(le|be)]
例：
java main.class 127.0.0.1 23456 le

# 使用
格式： 

  [二进制单元]|[二进制单元]|...
  
每个`[二进制单元]`的格式如下：

  [进制]:[长度（字节）]:[内容 内容 ]

示例：

二进制： b:4:11111100 11111101 11111110 11111111
八进制:  o:4:374 375 376 377
十进制： d:4:252 253 254 255
十六进制： x:4:fc fd fe ff

可以直接在标准输入(stdin）中输入示例内容，回车发送;
或者写入文件中，再用重定向或管道导入测试程序

java main.class 127.0.0.1 23456 le < test.txt
