package com.pydlove.frametalk.boot;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@MapperScan(
        basePackages = "com.pydlove.frametalk",
        annotationClass = Mapper.class)
@SpringBootApplication(
        scanBasePackages = "com.pydlove.frametalk",
        exclude = UserDetailsServiceAutoConfiguration.class)
public class FrameTalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrameTalkApplication.class, args);
    }
}
