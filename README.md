# Spring-Framework Decorator

## 介绍
众所周知，spring在支持装饰器模式上是有一定的灵活性的损耗的，如果一个接口含有两个实现类，并在某个组件中进行依赖，并且这个实现类没有通过@Primary或者@Qualifier进行指定bean的话会抛出异常，而在装饰器模式中存在类似的链式结构（我这装饰器模式实现有点类似责任链模式，不过大体差不多，行为是一致的），使用spring的灵活度就没有那么高了，所以为了满足偶现在的需求，此项目就有了背景。如果有必要的话可以自行拉去项目然后进行魔改，形成自己的版本，如果有bug，请提交Issue，欢迎大家一键三连。

## 使用方式
如果是springboot方式，在需要注入的接口上使用@Autowired注解，然后使用@WrapperProvider指定包装类既可，包装的类必须实现 BeanWrapper 接口，被包装的类（原始实现类）需要实现 WrapperBean 接口（其实也没啥用，就是做个标记）既可实现包装例如：
```java

public interface TestService {
    String get();
}

@Service
public class TestServiceImplOne implements TestService, WrapperBean{
    @Override
    public String get() {
        return "one";
    }
}

@Service
public class TestServoceImplOneT implements TestService, WrapperBean{
    @Override
    public String get() {
        return "oneT";
    }
}

@Component
public class TestServiceWrapper implements TestService, BeanWrapper {
    private TestService wrapper;

    @Override
    public String get() {
        return "wrapper(" + wrapper.get() + ")";
    }

    @Override
    public void wrapper(Object wrapperBean) {
        this.wrapper = (TestService) wrapperBean;
    }
}

@Service
public class TestServiceWrapperTwo implements TestService, BeanWrapper {

    private TestService wrapperBean;

    @Override
    public String get() {
        return "wrapperTwo(" + wrapperBean.get() + ")";
    }

    @Override
    public void wrapper(Object wrapperBean) {
        this.wrapperBean = (TestService) wrapperBean;
    }
}

@Controller
public class FilePreviewController{
    //@Qualifier("testServoceImplOneT") 此处可以加上 Qualifier 注解，也可以不用加，加入注解可以指定包裹
    // 的实现类，不指定默认在容器中寻找到当前类的Class所实现了BeanWrapper的实现类，例如，有两个类实现了TestService
    // 和WrapperBean接口，其中一个叫TestServiceImplOne另一个叫TestServoceImplOneT，那么没使用Qualifier
    // 指定的话是使用TestServiceImplOne (默认以spring扫描出第一个为准)，如果有Qualifier，那么以Qualifier
    // 指定bean为准，这个bean将会传递给实现了TestService和BeanWrapper的实现类也就是WrapperProvider中的指定
    // class的bean
    @Autowired
    @WrapperProvider(value = {
            TestServiceWrapper.class,
            TestServiceWrapperTwo.class
    })
    private TestService testService;
    
    @Autowired
    public void postInit(
        	// @Qualifier("emailService") 只有在此处加上 Qualifier 才生效，并且当前实现类的接口需要有多个实现
        	// 要不然只凭借 WrapperProvider 是不生效的
            @WrapperProvider(value = {
                    TestServiceWrapper.class,
                    TestServiceWrapperTwo.class,
                    TestServiceImplThree.class
            })
            EmailService emailService,
            //@Qualifier("testServoceImplOneT") 此处可以加上 Qualifier 注解，也可以不用加，加入注解可以指定包裹
        	// 的实现类，不指定默认在容器中寻找到当前类的Class所实现了BeanWrapper的实现类，例如，有两个类实现了TestService
        	// 和WrapperBean接口，其中一个叫TestServiceImplOne另一个叫TestServoceImplOneT，那么没使用Qualifier
        	// 指定的话是使用TestServiceImplOne (默认以spring扫描出第一个为准)，如果有Qualifier，那么以Qualifier
        	// 指定bean为准，这个bean将会传递给实现了TestService和BeanWrapper的实现类也就是WrapperProvider中的指定
        	// class的bean
            @WrapperProvider(value = {
                    TestServiceWrapper.class,
                    TestServiceWrapperTwo.class,
                    TestServiceImplThree.class
            })
            TestService testService
    ) {
        System.out.println(testService.get());
        System.out.println(emailService);
    }
    
}
```
示例中FilePreviewController下的testService实际上的实现类型是最后一个包装类，有点类似于mybatis的插件机制，执行的顺序是从下往上调用的。当然到了这里你以为高枕无忧了么？错了，其实还有一部分没有写，使用此模块需要修改启动类，替换其容器，替换容器是一定需要的，毕竟覆盖了DefaultListableBeanFactory的doResolveDependency，找了一圈都没找到如何动态的在容器中替换掉默认的bean。

启动类示例：

```java
@SpringBootApplication
public class Application {
	
	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication();
springApplication.setApplicationContextClass(AnnotationConfigServletWebServerApplicationContextPlus.class);
		springApplication.addPrimarySources(Collections.singletonList(Application.class));
		springApplication.run(args);
	}
}
```

> 注意：WrapperBean是被包裹的对象，BeanWrapper是包裹bean的对象，两个不要搞混了，虽然和BeanFactory与FactoryBean有点类似，但是本质是不同的。
