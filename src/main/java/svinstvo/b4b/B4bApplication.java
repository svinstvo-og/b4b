package svinstvo.b4b;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class B4bApplication {

	public static void main(String[] args) {
		SpringApplication.run(B4bApplication.class, args);
//		log.debug("B4bApplication started, openai key = {}, telegram api key = {}");
	}

}
