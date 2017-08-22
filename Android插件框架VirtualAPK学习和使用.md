##  Android插件框架VirtualAPK学习和使用

### 背景
近几年插件化比较火热，滴滴开源了自研的插件化框架VirtualAPK，作者是大名鼎鼎的任玉刚，他的：
博客地址:[http://blog.csdn.net/singwhatiwanna ]
GitHub地址:[https://github.com/singwhatiwanna ]

### 什么是插件化
插件（Plugin）在计算机领域是一个通用的概念，维基百科上这样解释：

>In computing, a plugin is a software component that adds a specific feature to an existing computer program.

意思是对现有的程序添加额外的功能组件。这样的好处是可以对原有程序进行扩展，以Android Studio为例，我们可以安装FindBugs插件，这样IDE就具备了更强大的静态代码检查功能。插件可以是第三方的开发者开发，只需要遵循一定的协议即可，插件可以随时更新、卸载，可以看到插件化使我们的程序更加灵活。

### 为什么要插件化？
插件化听起来很美好，但是谷歌并没有赋予Android这种能力，因为这样存在安全风险，试想一下表面上是一个计算器App，谷歌审核通过后，加载插件后变成一个窃取隐私的App怎么办。那为什么在国内插件化技术非常热门，各个大厂都在使用呢？总结了下有以下几个原因：

1. **国内App的版本碎片较为严重**，想减少升级成本。国内没有统一的应用分发市场，静默升级需要ROM的支持，否则第三方应用市场需要Root的方式实现；
2. **解决App方法数超65536问题**。在谷歌官方的Multidex方案没有出现时，可以采用插件方式解决，而现在该问题不应该是你选择插件化的原因；
3. **减少App包大小**。宿主App包含了主要功能，其余放到插件中实现，动态下发；
4. **快速修改线上Bug或者发布新功能**。作为程序员这个场景肯定遇到过，刚发布一个新版本存在Bug，再重新发版成本又特别高。而只发布插件成本就会很低，又能快速解决线上问题。
5. **模块解耦，协同开发**。一个超级App，可以拆分成不同的插件模块，基于一定的规则业务线之间协同开发，最终每个业务模块生成一个插件，由宿主加载组合即可。再比如你需要接入第三方App的功能，只需要第三方基于插件协议开发即可，不需要在宿主代码中进行开发，很好的做到业务隔离，合作终止时直接移除插件即可。

### 插件化现状
到目前为止，业界已经有很多优秀的开源项目，比如早期的基于静态代理思想的 DynamicLoadApk，随后的基于占坑思想的 DynamicApk、Small，还有360手机助手的 DroidPlugin。它们都是优秀的开源项目，很大程度上促进了国内插件化技术的发展，但是他们存在一些适配的问题。
滴滴为什么还要自研一款新的插件化框架？因为我们需要一款功能完备、兼容性优秀、适用于滴滴业务的插件化框架，目前市面上的开源不能满足我们的需求，所以必须重新造轮子，于是 VirtualAPK 诞生了。

### VirtualAPK介绍
github地址：[https://github.com/didi/VirtualAPK ]
我的例子github地址：[https://github.com/llayjun/MyVirtualApkDemo ]
VirtualAPK 是滴滴出行自研的一款优秀的插件化框架，主要有如下几个特性。

1. 功能完备
* 支持几乎所有的 Android 特性；
* 四大组件方面：四大组件均不需要在宿主manifest中预注册，每个组件都有完整的生命周期。
  * Activity：支持显示和隐式调用，支持 Activity 的 theme 和 LaunchMode，支持透明主题；
  * Service：支持显示和隐式调用，支持 Service 的 start、stop、bind 和 unbind，并支持跨进程 bind 插件中的 Service；
  * Receiver：支持静态注册和动态注册的 Receiver；
  * ContentProvider：支持 provider的所有操作，包括 CRUD 和 call 方法等，支持跨进程访问插件中的 Provider。
* 自定义View：支持自定义 View，支持自定义属性和 style，支持动画；
* PendingIntent：支持 PendingIntent 以及和其相关的 Alarm、Notification 和AppWidget；
* 支持插件 Application 以及插件 manifest 中的 meta-data；
* 支持插件中的so。

2. 优秀兼容性
* 兼容市面上几乎所有的 Android 手机，这一点已经在滴滴出行客户端中得到验证；
* 资源方面适配小米、Vivo、Nubia 等，对未知机型采用自适应适配方案；
* 极少的 Binder Hook，目前仅仅 hook 了两个 Binder：AMS 和 IContentProvider，Hook过程做了充分的兼容性适配；
* 插件运行逻辑和宿主隔离，确保框架的任何问题都不会影响宿主的正常运行。

3. 入侵性极低
* 插件开发等同于原生开发，四大组件无需继承特定的基类；
* 精简的插件包，插件可以依赖宿主中的代码和资源，也可以不依赖；
* 插件的构建过程简单，通过Gradle插件来完成插件的构建，整个过程对开发者透明。

### VirtualAPK 的工作过程
VirtualAPK 对插件没有额外的约束，原生的 apk 即可作为插件。插件工程编译生成 apk 后，即可通过宿主 App 加载，每个插件 apk 被加载后，都会在宿主中创建一个单独的 LoadedPlugin 对象。如下图所示，通过这些 LoadedPlugin 对象，VirtualAPK 就可以管理插件并赋予插件新的意义，使其可以像手机中安装过的App一样运行。

1. VirtualAPK 的运行形态
  滴滴计划赋予 VirtualAPK 两种工作形态，耦合形态和独立形态。目前 VirtualAPK 对耦合形态已经有了很好的支持，接下来将计划支持独立形态。
* **耦合形态：**插件对宿主可以有代码或者资源的依赖，也可以没有依赖。这种模式下，插件中的类不能和宿主重复，资源 id 也不能和宿主冲突。这是 VirtualAPK 的默认形态，也是适用于大多数业务的形态。
* **独立形态：**插件对宿主没有代码或者资源的依赖。这种模式下，插件和宿主没有任何关系，所以插件中的类和资源均可以和宿主重复。这种形态的主要作用是用于运行一些第三方 apk

### VirtualAPK框架的简单使用
#### 宿主工程的接入
宿主工程需要如下几步：
1. 在Project主工程根目录的build.gradle添加依赖
```
dependencies {
 classpath 'com.didi.virtualapk:gradle:0.9.0'
}
```
2. 在app的工程模块的build.gradle添加使用gradle插件
```
apply plugin: 'com.didi.virtualapk.host'
```
3. 在app的工程模块的build.gradle添加VirtualAPK SDK compile依赖
```
dependencies {
 compile 'com.didi.virtualapk:core:0.9.0'
}
```
4. 在App的工程模块proguard-rules.pro文件添加混淆规则
```
-keep class com.didi.virtualapk.internal.VAInstrumentation { *; }
-keep class com.didi.virtualapk.internal.PluginContentResolver { *; }

-dontwarn com.didi.virtualapk.**
-dontwarn android.content.pm.**
-keep class android.** { *; }
```
5. MyApplication类是继承了Application，覆写attachBaseContext函数，进行插件SDK初始化工作
```
@Override
protected void attachBaseContext(Context base)  {
   super.attachBaseContext(base);
   PluginManager.getInstance(base).init();
}
```
6. 在使用插件之前加载插件，可以根据具体业务场景选择合适时机加载，我是在MainActivity的onCreate时机加载
```
private void loadPlugin() {
//注意在Androidmanifest.xml文件中添加文件读取权限
String _path = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/" + mApkName);
File _file = new File(_path);
mFileExists = _file.exists();
if (mFileExists) {
try {
  PluginManager.getInstance(this).loadPlugin(_file);
} catch (Exception _e) {
  _e.printStackTrace();
}
}
}
```
*经过上述6步后，VirtualAPK插件功能就集成到宿主中了，宿主打包和运行方式没有任何改变。接下来看下插件工程如何集成和构建的。*

#### 插件工程的接入
插件工程需要如下几步：
1. **检查环境配置**
* Gradle版本需要为 **2.14.1**，可以使用gradle -v查看环境中配置的Gradle版本号。也可以使用工程中gradlew来编译，可以在gradle/wrapper/gradle-wrapper.properties中更改版本号
  **distributionUrl=https\://services.gradle.org/distributions/gradle-2.14.1-all.zip**
* com.android.tools.build的版本号为 **2.1.3**
2. 插件工程的Project工程的build.gradle添加依赖
```
dependencies {
 classpath 'com.didi.virtualapk:gradle:0.9.0'
}
```
3. 插件app工程的build.gradle添加
```
apply plugin: 'com.didi.virtualapk.plugin'
//放在文件最下面
virtualApk {
 packageId = 0x6f             // 插件资源id，避免资源id冲突
 targetHost='../myhost/app'      // 宿主工程的路径
 applyHostMapping = true      // 插件编译时是否启用应用宿主的apply mapping
}
```

解释一下上面3个参数的作用

1. packageId用于定义每个插件的资源id，多个插件间的资源Id前缀要不同，避免资源合并时产生冲突
2. targetHost指明宿主工程的应用模块，插件编译时需要获取宿主的一些信息，比如mapping文件、依赖的SDK版本信息、R资源文件，一定不能填错，否则在编译插件时会提示找不到宿主工程。
3. applyHostMapping表示插件是否开启apply mapping功能。当宿主开启混淆时，一般情况下插件就要开启applyHostMapping功能。因为宿主混淆后函数名可能有fun()变为a()，插件使用宿主混淆后的mapping映射来编译插件包，这样插件调用fun()时实际调用的是a()，才能找到正确的函数调用。

4. 最后一步
  生成插件，需要使用Gradle命令
```
gradle clean assemblePlugin
或者
./gradlew clean assemblePlugin
```
也可在AS右侧，选择assemblePlugin，如图：

编译完成之后，在build包中，会生成plugin中的包，如图：

**注意** 
* 插件包均是Release包，不支持debug模式的插件包
* 如果存在多个productFlavors，那么将会构建出多个插件包

### 运行工程
1. 因为宿主项目中是读取文件中apk插件
```
String _path = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/" + mApkName);
```
则将plugin包中的插件apk包放到手机对应的文件下。
2. 重新启动应用，点击按钮
```
if (mFileExists) {
  Intent _intent = new Intent();
  _intent.setClassName(mPackageName, mClassName);
  startActivity(_intent);
  } else {
  Toast.makeText(this, "文件包不存在!", Toast.LENGTH_SHORT).show();
  }
```
3. 注意点
* 要想清楚宿主和插件的业务边界很重要，才能找到插件的入口点。Demo中是以ImageBrowserActivity为边界，从这个Activity之后的功能来来自于插件中。其实我们可以把插件看成一个类提供者，可以使用Class.forName()这种方式使用插件中的类，所以不是只能从Activity/Fragment作为入口点。
* 编译宿主工程时会生成一些信息（在build/VAHost文件夹下），插件构建时会读取这些信息，所以要确保运行的宿主和插件基于相同信息构建的，宿主变化时请重新构建插件。

### 常见问题
在滴滴github上列举出部分常见问题：github地址：[https://github.com/didi/VirtualAPK/wiki/%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98%E8%A7%A3%E7%AD%94 ]


