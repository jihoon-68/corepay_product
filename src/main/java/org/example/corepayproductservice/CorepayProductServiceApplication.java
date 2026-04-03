package org.example.corepayproductservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CorepayProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorepayProductServiceApplication.class, args);
    }

}
