#ServiceFramework for Maven

[原始文档](https://github.com/allwefantasy/ServiceFramework)

对于一个以Maven为基础工具的开发团队，使用另外一种目录结构毕竟是不方便的(即使这个结构很简单)。而且一个项目没有版本号，没有依赖库的版本管理，又带来另外的一丝丝的不安全感。好在有了git，这一切都变得简单很多。

改动：
创建example目录，把src/com移至example/src/main/java，把test/test移至example/src/test/java
.
├── active_orm
├── csdn_common
├── mongomongo
├── ServiceFramework
└── ServiceFramework4Maven

项目依赖的其它模块：[csdn_common](https://github.com/cheney83/csdn_common)、[active_orm](https://github.com/cheney83/csdn_common)、[mongomongo](https://github.com/cheney83/mongomongo)，也都提交了一个pom.xml来管理。

当前进度compile：
cd ServiceFramework4Maven
mvn compile