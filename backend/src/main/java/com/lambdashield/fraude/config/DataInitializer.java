package com.lambdashield.fraude.config;

import com.lambdashield.fraude.config.seed.CatalogoDemoSeeder;
import com.lambdashield.fraude.config.seed.ClienteDemoSeeder;
import com.lambdashield.fraude.config.seed.EvaluacionDemoSeeder;
import com.lambdashield.fraude.config.seed.UsuarioDemoSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Coordina la carga idempotente de los datos de demostración al iniciar el backend. */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final CatalogoDemoSeeder catalogoSeeder;
    private final ClienteDemoSeeder clienteSeeder;
    private final UsuarioDemoSeeder usuarioSeeder;
    private final EvaluacionDemoSeeder evaluacionSeeder;

    public DataInitializer(CatalogoDemoSeeder catalogoSeeder,
                           ClienteDemoSeeder clienteSeeder,
                           UsuarioDemoSeeder usuarioSeeder,
                           EvaluacionDemoSeeder evaluacionSeeder) {
        this.catalogoSeeder = catalogoSeeder;
        this.clienteSeeder = clienteSeeder;
        this.usuarioSeeder = usuarioSeeder;
        this.evaluacionSeeder = evaluacionSeeder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        catalogoSeeder.seedRoles();
        clienteSeeder.seed();
        catalogoSeeder.seedCatalogos();
        catalogoSeeder.seedReglas();
        usuarioSeeder.seed();
        evaluacionSeeder.seed();
    }
}
