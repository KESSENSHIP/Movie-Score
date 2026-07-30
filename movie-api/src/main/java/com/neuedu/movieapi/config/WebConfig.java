package com.neuedu.movieapi.config;

import com.neuedu.movieapi.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 注释 @Configuration 可以停用JWT认证
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/movies/**", "/api/users/**", "/api/comments/**", "/api/ratings/**", "/api/persons/**", "/api/reviews/**", "/api/sys-users/**", "/api/user-history/**", "/api/stats/**", "/api/predictions/**", "/api/user-profile/**", "/api/recommendations/**", "/api/trend-forecast/**"); // 公开接口可放行
    }
}