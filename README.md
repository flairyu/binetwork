# binetwork

用于测试二进制协议的网络通讯客户端。

# 编译
进入src目录
```
  javac ruanmianbao/binetwork/Main.java
```

# 运行
进入src目录
```
  java ruanmianbao.binetwork.Main [server ip or name] [port] [byteorder(le|be)]
```
例：
```
  java ruanmianbao.binetwork.Main 127.0.0.1 23456 le
```

# 使用
格式： 
```
  [二进制单元]|[二进制单元]|...
```
每个`[二进制单元]`的格式如下：
```
  [进制]:[单位（1-byte 2-short 4-int 8-long]:[内容 内容 ]
```
示例：

```
二进制：  	b:1:11111100 11111101 11111110 11111111
八进制:   	o:2:176375 177377
十进制：   	d:1:252 253 254 255
十六进制：  	x:4:fcfdfeff
字符串: 		s:utf-8:"Hello world! and 中文"
```

组合：
```
b:1:11111100 11111101 11111110 11111111|o:2:176375 177377|d:1:252 253 254 255|x:4:fcfdfeff
```

可以直接在标准输入(stdin）中输入示例内容，回车发送;
或者写入文件中，再用重定向或管道导入测试程序

```
java ruanmianbao.binetwork.Main 127.0.0.1 23456 le < test.txt
```
