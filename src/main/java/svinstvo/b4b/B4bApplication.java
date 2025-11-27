package svinstvo.b4b;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import svinstvo.b4b.config.OpenAIConfig;

@SpringBootApplication
@Slf4j
public class B4bApplication {

	public static void main(String[] args) {
		SpringApplication.run(B4bApplication.class, args);
	}

}
