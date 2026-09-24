package vn.edu.sales;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("vn.edu.sales");

    static final ArchRule businessLayersDoNotDependOnAdapters = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..api..", "..infrastructure..", "..config..");

    static final ArchRule businessLayersDoNotDependOnWebOrDatabaseFrameworks = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..");

    static final ArchRule apiDoesNotAccessPersistenceImplementation = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..");

    @Test
    void businessLayerIsIndependentOfApiAndAdapters() {
        businessLayersDoNotDependOnAdapters.check(CLASSES);
    }

    @Test
    void businessLayerDoesNotImportWebOrDatabaseFrameworks() {
        businessLayersDoNotDependOnWebOrDatabaseFrameworks.check(CLASSES);
    }

    @Test
    void apiDoesNotImportPersistenceAdapters() {
        apiDoesNotAccessPersistenceImplementation.check(CLASSES);
    }
}
