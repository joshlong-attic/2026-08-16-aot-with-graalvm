package com.example.aot;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.*;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;


// > jdk proxies 
// > reflection
// > serialization
// > jni
// > resources loading (.txt, .ico, .properties)

@Import(MyBeanRegistrar.class)
@Controller
@ResponseBody
@ImportRuntimeHints(AotApplication.Hints.class)
@SpringBootApplication
public class AotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AotApplication.class, args);
    }

    @GetMapping("/")
    Map<String, String> home() {
        return Map.of("message", "Hello World");
    }

    private final Customer john = new Customer(1, "John");

    private static final Resource FILE = new ClassPathResource("/message");

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.reflection().registerType(Customer.class, MemberCategory.values());
            hints.resources().registerResource(FILE);
        }
    }

    @Bean
    ApplicationRunner runner(JsonMapper jsonMapper) {
        return _ -> {
            IO.println("content:" + FILE.getContentAsString(Charset.defaultCharset()));
            IO.println(jsonMapper.writeValueAsString(john));
        };
    }
}

@Configuration
class MyCOnfig {

    @Bean
    static MyBeanFactoryInitializationAotProcessor myBeanFactoryInitializationAotProcessor() {
        return new MyBeanFactoryInitializationAotProcessor();
    }
}

class MyBeanFactoryInitializationAotProcessor implements
        BeanFactoryInitializationAotProcessor {
    
    @Override
    public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {
        var processedClasses = new HashSet<Class<?>>();
        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var type = beanFactory.getType(beanName);
            if (type.isAnnotationPresent(Flow.class)) {
                processedClasses.add(type);
            }
        }
        return (generationContext, beanFactoryInitializationCode) -> {
            for (var pc : processedClasses) {
                var runtimeHints = generationContext.getRuntimeHints();
                runtimeHints.reflection().registerType(TypeReference.of(pc.getName()));
                var resource = this.optionalResourceFor(pc);
                if (resource.exists()) {
                    runtimeHints.resources().registerResource(resource);
                }
            }
        };
    }

    private Resource optionalResourceFor(Class<?> clzz) {
        var clzzName = clzz.getName();
        var path = clzzName.replace('.', '/') + ".txt";
        return new ClassPathResource(path);
    }
}

@Flow
class MyFlow {
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface Flow {
}

class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var beanDefinition = beanFactory.getBeanDefinition(beanName);
            var clzz = beanFactory.getType(beanName);
            IO.println("============================================================================");
            IO.println(clzz.getName());
            ReflectionUtils.doWithMethods(Objects.requireNonNull(clzz),
                    (method) -> IO.println("\t" + method));
        }
    }
}


class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(LogginApplicationRunner.class);
    }
}

class LogginApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        IO.println("hi!");
    }
}

record Customer(int id, String name) {
}


// BeanDefinition
