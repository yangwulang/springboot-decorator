package top.yangwulang.decorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodParameter;
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
        MethodParameter methodParameter = descriptor.getMethodParameter();
        Qualifier qualifier;
        WrapperProvider wrapperProvider;
        if (field != null
                && (qualifier = field.getAnnotation(Qualifier.class)) != null
                && (wrapperProvider = field.getAnnotation(WrapperProvider.class)) != null
        ) {
            Object bean = getBean(qualifier.value());
            wrapperBean = wrapper(bean, wrapperProvider.value());
        } else if (methodParameter != null
                && (qualifier = methodParameter.getParameterAnnotation(Qualifier.class)) != null
                && (wrapperProvider = methodParameter.getParameterAnnotation(WrapperProvider.class)) != null
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
        MethodParameter methodParameter = descriptor.getMethodParameter();
        WrapperProvider annotation;
        Class<?> type;
        if (field != null ) {
            annotation = field.getAnnotation(WrapperProvider.class);
            type = field.getType();
        } else if (methodParameter != null) {
            annotation = methodParameter.getParameterAnnotation(WrapperProvider.class);
            type = methodParameter.getParameterType();
        } else {
            throw e;
        }
        //WrapperProvider[] annotationsByType = field.getAnnotationsByType(WrapperProvider.class);
        if (annotation == null) {
            throw e;
        }
        Class<? extends BeanWrapper>[] value = annotation.value();
        Map<String, ?> beansOfType = getBeansOfType(type);

        // ?????????????????????????????????
        List<WrapperBean> beanWrappers = beansOfType.values()
                .stream()
                .filter(implType -> ClassUtils.isAssignable(WrapperBean.class, implType.getClass()))
                .map(implType -> (WrapperBean) implType)
                .collect(Collectors.toList());
        if (beanWrappers.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(type, 0, "?????????????????? WrapperBean ??????????????????");
        }
        return wrapper(beanWrappers.get(0), value);
    }

    private Object wrapper(Object wrapperBean, Class<? extends BeanWrapper>[] value) {
        //????????????????????????????????????????????????
        BeanWrapper bean = null;
        BeanWrapper preBean = null;
        for (Class<? extends BeanWrapper> beanWrapperClass : value) {
            if (bean != null) {
                preBean = bean;
            }
            // ??????????????????????????????????????????????????? getBean ????????????????????????
            Map<String, ? extends BeanWrapper> realWrapper = getBeansOfType(beanWrapperClass);
            List<? extends BeanWrapper> realWrapperList = realWrapper
                    .values()
                    .stream()
                    .filter(r -> r.getClass().equals(beanWrapperClass))
                    .collect(Collectors.toList());
            if (realWrapperList.size() > 1) {
                throw new NoUniqueBeanDefinitionException(beanWrapperClass, realWrapper.size(), "??????????????????????????????????????????");
            } else if (realWrapperList.size() == 0) {
                throw new NoUniqueBeanDefinitionException(beanWrapperClass, 0, "????????????????????????????????????????????????????????????????????? BeanWrapper ?????????????????????????????????");
            }
            bean = realWrapperList.get(0);
            if (preBean == null) {
                bean.wrapper(wrapperBean);
            } else {
                bean.wrapper(preBean);
            }
        }
        // ????????????????????????????????????????????? WrapperProvider ????????????????????????????????????????????????
        return bean == null ? wrapperBean : bean;
    }
}
