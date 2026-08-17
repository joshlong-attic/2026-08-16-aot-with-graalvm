package com.example.demo;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.stereotype.Component;

@ImportRuntimeHints(DemoApplication.Hints.class)
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    @Bean
    ApplicationRunner service(CustomerService customerService) {
        return args -> {
            for (var i : customerService.getClass().getInterfaces())
                IO.println("i:" + i);
            var customer = customerService.getCustomer("Jane");
            IO.println(customer);
        };
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            // only support interfaces!
            hints.proxies().registerJdkProxy(CustomerService.class, 
                    SpringProxy.class, Advised.class, DecoratingProxy.class);
        }
    }

    @Bean
    CustomerService customerService() {
        var proxy = new ProxyFactoryBean();
        proxy.addInterface(CustomerService.class);
        proxy.addAdvice(new MethodInterceptor() {
            @Override
            public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
                var method = invocation.getMethod();
                var args = invocation.getArguments();
                if (method.getName().equals("getCustomer")) {
                    for (var a : args)
                        IO.println("arguments: " + a);
                    return new Customer("John", "");
                }
                return null;
            }
        });
        return (CustomerService) proxy.getObject();
        /*var proxy = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{CustomerService.class}, (proxy1, method, args) -> {
                    if (method.getName().equals("getCustomer")) {
                        for (var a : args)
                            IO.println("arguments: " + a);
                        return new Customer("John", "");
                    }
                    return null;
                });
        return (CustomerService) proxy;*/
    }
}
//class MyCustomerService implements CustomerService {
//
//    @Override
//    public Customer getCustomer(String name) {
//        return null;
//    }
//}

@Component
class MyCustomerService {
    
    
}

interface CustomerService {
    Customer getCustomer(String name);
}

record Customer(String name, String email) {
}