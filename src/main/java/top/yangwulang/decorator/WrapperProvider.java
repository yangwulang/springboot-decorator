package top.yangwulang.decorator;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapperProvider {
    Class<? extends BeanWrapper>[] value() default {};
}
