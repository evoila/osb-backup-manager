package de.evoila.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 
 * @author Johannes Hiemer.
 *
 */
@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = "de.evoila.cf",
        excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern="de\\.evoila\\.cf\\.broker\\.service\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern="de\\.evoila\\.cf\\.broker\\.controller\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern="de\\.evoila\\.cf\\.config\\.security\\..*")
})
public class Application implements WebMvcConfigurer {

    static Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedHeaders("*")
                .exposedHeaders("WWW-Authenticate",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Headers"
                )
                .allowedMethods("OPTIONS", "HEAD",
                        "GET", "POST",
                        "PUT", "PATCH",
                        "DELETE", "HEAD")
                .allowCredentials(true);
    }

}