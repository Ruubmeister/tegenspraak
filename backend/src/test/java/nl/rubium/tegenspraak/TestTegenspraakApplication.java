package nl.rubium.tegenspraak;

import org.springframework.boot.SpringApplication;

public class TestTegenspraakApplication {

    public static void main(String[] args) {
        SpringApplication.from(TegenspraakApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
