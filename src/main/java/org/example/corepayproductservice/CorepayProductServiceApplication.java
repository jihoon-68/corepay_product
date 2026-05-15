package org.example.corepayproductservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "org.example.corepayproductservice",
        "org.example.corepaycommon"
})
public class CorepayProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorepayProductServiceApplication.class, args);
    }

}
