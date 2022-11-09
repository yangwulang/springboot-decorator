package top.yangwulang.decorator;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;

public class AnnotationConfigServletWebServerApplicationContextPlus
        extends AnnotationConfigServletWebServerApplicationContext {
    public AnnotationConfigServletWebServerApplicationContextPlus() {
        this(new DefaultListableBeanFactoryPlus());
    }

    public AnnotationConfigServletWebServerApplicationContextPlus(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }
}
