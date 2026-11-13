package nl.rubium.tegenspraak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TegenspraakApplication {

    public static void main(String[] args) {
        SpringApplication.run(TegenspraakApplication.class, args);
    }
}
