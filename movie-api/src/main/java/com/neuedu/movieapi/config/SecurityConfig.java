package com.neuedu.movieapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()   // 所有请求均放行
                )
                .csrf(csrf -> csrf.disable())   // 如不需要 CSRF 防护
                .headers(headers -> headers.disable()) // 可选
                .formLogin(form -> form.disable()) // 禁用表单登录
                .httpBasic(basic -> basic.disable()); // 禁用 HTTP Basic
        return http.build();
    }
}