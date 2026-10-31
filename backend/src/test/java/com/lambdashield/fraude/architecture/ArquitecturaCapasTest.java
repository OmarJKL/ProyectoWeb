package com.lambdashield.fraude.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Reglas de N capas del backend, verificadas sobre el bytecode de producción.
 *
 * Capas: presentation -> application -> domain, con persistence (repository) en la base.
 * config es la raíz de composición y puede usar todas las capas.
 */
@AnalyzeClasses(packages = "com.lambdashield.fraude", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaCapasTest {

    @ArchTest
    static final ArchRule respetaLasCapas = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Presentacion").definedBy("..presentation..")
            .layer("Aplicacion").definedBy("..application..")
            .layer("Dominio").definedBy("..domain..")
            .layer("Persistencia").definedBy("..repository..")
            .layer("Configuracion").definedBy("..config..")
            .whereLayer("Presentacion").mayOnlyBeAccessedByLayers("Configuracion")
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Presentacion", "Configuracion")
            .whereLayer("Persistencia").mayOnlyBeAccessedByLayers("Aplicacion", "Configuracion")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Persistencia", "Configuracion")
            .whereLayer("Configuracion").mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule dominioSinSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule aplicacionSinDetallesWeb = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.servlet..", "org.springframework.web..", "org.springframework.ui..");

    @ArchTest
    static final ArchRule controladoresEnPresentacion = classes()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule controladoresSinRepositorios = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule sinCiclosEntreModulos = slices()
            .matching("com.lambdashield.fraude.(*)..")
            .should().beFreeOfCycles();
}
