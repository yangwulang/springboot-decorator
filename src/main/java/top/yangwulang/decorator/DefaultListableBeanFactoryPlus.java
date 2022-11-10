package top.yangwulang.decorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultListableBeanFactoryPlus extends DefaultListableBeanFactory {
    private final Logger logger = LoggerFactory.getLogger(DefaultListableBeanFactoryPlus.class);

    @Override
    public Object doResolveDependency(DependencyDescriptor descriptor,
                                      String beanName,
                                      Set<String> autowiredBeanNames,
                                      TypeConverter typeConverter) throws BeansException {
        try {
            Object wrapperBean = super.doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
            return processQualifierDependency(descriptor, beanName, autowiredBeanNames, typeConverter, wrapperBean);
        } catch (NoUniqueBeanDefinitionException e) {
            return processNoQualifierDependency(descriptor, beanName, autowiredBeanNames, typeConverter, e);
        }
    }

    private Object processQualifierDependency(
            DependencyDescriptor descriptor,
            String beanName,
            Set<String> autowiredBeanNames,
            TypeConverter typeConverter,
            Object wrapperBean
    ) throws BeansException {
        Field field = descriptor.getField();
        Qualifier qualifier;
        WrapperProvider wrapperProvider;
        if (field != null
                && (qualifier = field.getAnnotation(Qualifier.class)) != null
                && (wrapperProvider = field.getAnnotation(WrapperProvider.class)) != null
        ) {
            Object bean = getBean(qualifier.value());
            wrapperBean = wrapper(bean, wrapperProvider.value());
        }
        return wrapperBean;
    }

    private Object processNoQualifierDependency(
            DependencyDescriptor descriptor,
            String beanName,
            Set<String> autowiredBeanNames,
            TypeConverter typeConverter,
            NoUniqueBeanDefinitionException e
    ) throws BeansException {
        Field field = descriptor.getField();
        if (field == null) {
            throw e;
        }
        //WrapperProvider[] annotationsByType = field.getAnnotationsByType(WrapperProvider.class);
        WrapperProvider annotation = field.getAnnotation(WrapperProvider.class);
        if (annotation == null) {
            throw e;
        }
        Class<? extends BeanWrapper>[] value = annotation.value();
        Map<String, ?> beansOfType = getBeansOfType(field.getType());

        // 找出所有被包裹的类实例
        List<WrapperBean> beanWrappers = beansOfType.values()
                .stream()
                .filter(implType -> ClassUtils.isAssignable(WrapperBean.class, implType.getClass()))
                .map(implType -> (WrapperBean) implType)
                .collect(Collectors.toList());
        if (beanWrappers.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(field.getType(), 0, "找不到实现了 WrapperBean 装饰器的对象");
        }
        return wrapper(beanWrappers.get(0), value);
    }

    private Object wrapper(Object wrapperBean, Class<? extends BeanWrapper>[] value) {
        //进行一层一层的包裹，包裹顺序是从
        BeanWrapper bean = null;
        BeanWrapper preBean = null;
        for (Class<? extends BeanWrapper> beanWrapperClass : value) {
            if (bean != null) {
                preBean = bean;
            }
            // 这里兜底防止有实现类进行继承，导致 getBean 会扫出多个实现类
            Map<String, ? extends BeanWrapper> realWrapper = getBeansOfType(beanWrapperClass);
            List<? extends BeanWrapper> realWrapperList = realWrapper
                    .values()
                    .stream()
                    .filter(r -> r.getClass().equals(beanWrapperClass))
                    .collect(Collectors.toList());
            if (realWrapperList.size() > 1) {
                throw new NoUniqueBeanDefinitionException(beanWrapperClass, realWrapper.size(), "此实现类居然多个实现类！！！");
            } else if (realWrapperList.size() == 0) {
                throw new NoUniqueBeanDefinitionException(beanWrapperClass, 0, "哦我的老天啊，这是不可能出现的异常，当前实现了 BeanWrapper 但是容器中不存在！！！");
            }
            bean = realWrapperList.get(0);
            if (preBean == null) {
                bean.wrapper(wrapperBean);
            } else {
                bean.wrapper(preBean);
            }
        }
        // 此处兜底，如果在字段上没有找到 WrapperProvider 注解就亏大了，导致此处返回空指针
        return bean == null ? wrapperBean : bean;
    }
}
