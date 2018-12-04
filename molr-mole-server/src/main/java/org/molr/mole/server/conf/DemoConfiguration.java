package org.molr.mole.server.conf;

import org.molr.mole.core.api.Mole;
import org.molr.mole.core.demo.DemoMole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoConfiguration {

    @Bean
    public Mole mole(){
        return new DemoMole();
    }

}
