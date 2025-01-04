package com.yoganavi.user.common.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(cors -> cors.disable());
        http.csrf(csrf -> csrf.disable());

        // JWT 토큰 검증 필터 추가
//        http.addFilterBefore(jwtTokenValidatorFilter(), BasicAuthenticationFilter.class);
//        http.addFilterBefore(apiKeyAuthFilter(), BasicAuthenticationFilter.class);

        // URL 기반 권한 부여 설정
        http.authorizeHttpRequests((requests) -> requests
            // 선생만 접근 가능
            .requestMatchers("/mypage/notification/write",
                "/mypage/notification/update/**",
                "/mypage/notification/delete/**",
                "/mypage/live-lecture-manage/**",
                "/mypage/recorded-lecture/list",
                "/mypage/recorded-lecture/create",
                "/mypage/recorded-lecture/detail/**",
                "/mypage/recorded-lecture/update/**",
                "/mypage/recorded-lecture/delete").hasRole("TEACHER")

            // 모든 인증된 사용자
            .requestMatchers("/mypage/notification/list",
                "/fcm",
                "/mypage/course-history",
                "/home",
                "/mypage/info",
                "/mypage/check",
                "/mypage/update",
                "/mylogout",
                "/delete",
                "/mypage/recorded-lecture/likelist",
                "/recorded-lecture/detail/**",
                "/recorded-lecture/like/**",
                "/recorded-lecture/sort/**",
                "/recorded-lecture/search/**",
                "/teacher/**").hasAnyRole("TEACHER", "STUDENT")

            // 모두에게 열려있다!
            .requestMatchers("/members/**",
                "/is-on").permitAll()

            .requestMatchers("/home/update").hasRole("SIGNAL")

            // 그 외의 경우
            .anyRequest().hasAnyRole("TEACHER", "STUDENT")
        );

        // HTTP 기본 인증 구성
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> new HashSet<>(authorities);
    }
}
