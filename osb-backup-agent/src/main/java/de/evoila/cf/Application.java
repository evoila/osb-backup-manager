package de.evoila.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * 
 * @author Johannes Hiemer.
 *
 */
@SpringBootApplication
@EnableAsync
public class Application {
	static Logger logger = LoggerFactory.getLogger(Application.class);
	public static void main(String[] args) {
		loadBinaries();
		SpringApplication.run(Application.class, args);
	}

	private static void loadBinaries () {
		File f = new File(Application.class.getResource("/startup.sh").getFile());
		try {
			logger.info("Downloading binaries");
			Runtime.getRuntime().exec("chmod +x "+ f.getAbsolutePath());
			ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath(), "/home/vcap/app/");
			Process process = pb.start();

		} catch (IOException e) {
			logger.info("An error occured downloading the backup binaries");
		}
	}


	@Bean
	public FilterRegistrationBean corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowCredentials(true);
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("OPTIONS");
		config.addAllowedMethod("HEAD");
		config.addAllowedMethod("GET");
		config.addAllowedMethod("PUT");
		config.addAllowedMethod("POST");
		config.addAllowedMethod("DELETE");
		config.addAllowedMethod("PATCH");
		source.registerCorsConfiguration("/**", config);

		final FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
		bean.setOrder(0);
		return bean;
	}

	@Bean
	public Executor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("BackupAgent-");
		executor.initialize();
		return executor;
	}

}