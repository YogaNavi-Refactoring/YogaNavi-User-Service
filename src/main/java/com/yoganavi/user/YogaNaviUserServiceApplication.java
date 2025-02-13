package com.yoganavi.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.yoganavi.user",
    "com.yoganavi.kafka"
})
@EnableJpaRepositories(basePackages = {
    "com.yoganavi.user.common.repository",
    "com.yoganavi.kafka.repository"
})
@EntityScan(basePackages = {
    "com.yoganavi.user.common.entity",
    "com.yoganavi.kafka.entity"
})
public class YogaNaviUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YogaNaviUserServiceApplication.class, args);
    }

}
