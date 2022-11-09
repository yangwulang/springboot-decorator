package top.yangwulang.decorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
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
            return super.doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
        } catch (NoUniqueBeanDefinitionException e) {
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
            //进行一层一层的包裹，包裹顺序是从
            BeanWrapper bean = null;
            BeanWrapper preBean = null;

            boolean isFirst;
            for (Class<? extends BeanWrapper> beanWrapperClass : value) {
                if (bean == null) {
                    isFirst = true;
                } else {
                    isFirst = false;
                    preBean = bean;
                }
                bean = getBean(beanWrapperClass);
                if (isFirst) {
                    bean.wrapper(beanWrappers.get(0));
                } else {
                    bean.wrapper(preBean);
                }
            }
            return bean;
        }
    }
}
