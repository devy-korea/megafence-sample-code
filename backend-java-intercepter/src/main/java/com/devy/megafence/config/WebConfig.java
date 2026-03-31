package com.devy.megafence.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import com.devy.megafence.interceptor.WebGateInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private WebGateInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**");
    }
}
