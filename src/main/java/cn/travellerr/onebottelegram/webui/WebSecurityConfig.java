package cn.travellerr.onebottelegram.webui;

import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(TelegramOnebotAdapter.config.getOnebot().getPath()).permitAll()
            .requestMatchers("/ws/**").permitAll()
            .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
//                .addFilterBefore(new OncePerRequestFilter() {
//                    @Override
//                    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
//                        System.out.println("Received request: " + request.getMethod() + " " + request.getRequestURI());
//                        filterChain.doFilter(request, response);
//                    }
//                }, SecurityContextHolderAwareRequestFilter.class);
        http.csrf(AbstractHttpConfigurer::disable);
        http.rememberMe(rememberMe -> rememberMe
                .tokenValiditySeconds(86400*7) // 7天
        );

        return http.build();
    }


    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername(TelegramOnebotAdapter.config.getSpring().getWebui().getUserName())
                        .password(passwordEncoder().encode(TelegramOnebotAdapter.config.getSpring().getWebui().getPassword()))
                        .roles("ADMIN")
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        // 使用BCrypt加密密码
        return new BCryptPasswordEncoder();
    }
}
