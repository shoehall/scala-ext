# 简介
有些轮子在java/scala技术栈确实是没有, 在使用中频繁切换到python又不方便, 因此造轮子开始...
## 项目目录
- [rich-scala](./rich-scala/README.md)    
  主要对scala进行进一步扩展的工具类, 主要使用隐式转换增加原有类型的方法。
  
- sciscala     
  基于scala的科学计算, 将scipy中一些功能点搬进来, 方便在scala中调用.
- spandas     
  结合了spark-sql和pandas的一些特性的结构化数据操作库。
- doc
  应用文档.

## road map
### rich-scala
- 增加一个pair iterator, 内置分组聚合的功能
- 时间类的delta类型
- 时间字符串解析和format类
  
### spandas
- 一个IO工具类, 或者作为scala source的扩展也行.
- 设计基础的immutable seq类型
- 实现slice风格的整数索引
- 实现Slice索引
- 设计一个DataFrame类型和Row类型
- index类
- mutable seq类
- factor immutableSeq对象
- 类型转换  
- 排序方法
- 单个DataFrame的形变操作
- 单个DataFrame的聚合操作
- DataFrame和Series之间的操作
- DataFrame和DataFrame之间的操作

## rich-scala

```markdown
## 1
```







<iframe  
height=850
width=90%
src=./rich-scala/README.md
frameborder=0  
allowfullscreen>
</iframe>

## rich-scala
