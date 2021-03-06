package io.molr.mole.core.conf;

import io.molr.mole.core.api.Mole;
import io.molr.mole.core.local.LocalSuperMole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Set;

@Configuration
public class LocalSuperMoleConfiguration {

    @Autowired
    private Set<Mole> moles;

    @Bean
    @Primary
    public LocalSuperMole superMole() {
        return new LocalSuperMole(moles);
    }

}
