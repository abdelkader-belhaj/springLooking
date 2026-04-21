package tn.hypercloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookingApplication.class, args);
    }

}
