## 配置数据源
在gameds.properties配置数据源


## 实体类使用方法

### 1、通用实体类
例如：
```
@PersistName
@PersistPeriod
@PersistDaoTemplate(PersistDao.class)
public class RoleExample implements Persistable {
    @Tag(1)
    private long id;
   	@tag(2)
    private String name;
}
```
- @PersistName   自定义表名，默认为SimpleClassName
- @PersistPeriod 指定操作间隔 默认为60*1000 
- @PersistDaoTemplate 参数为继承PersistDao（抽象类）的类 

启动游戏服务器时将扫描server模块下包含@PersistDaoTemplate(PersistDao.class) 注解的类
会根据PersistDao的具体实现类中的ddl语句创建数据表

注意：

2. 使用
```
     DataCenter.get();
     DataCenter.update();
```