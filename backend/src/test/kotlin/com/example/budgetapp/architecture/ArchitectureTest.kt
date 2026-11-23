package com.example.budgetapp.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

class ArchitectureTest {

    @Test
    fun `クリーンアーキテクチャの4層の依存方向が外側から内側へのみ向かっていること`() {
        Konsist
            .scopeFromProject()
            .assertArchitecture {
                val domain = Layer("Domain", "com.example.budgetapp.domain..")
                val usecase = Layer("UseCase", "com.example.budgetapp.usecase..")
                val adapter = Layer("Adapter", "com.example.budgetapp.adapter..")
                val framework = Layer("Framework", "com.example.budgetapp.framework..")

                domain.dependsOnNothing()
                usecase.dependsOn(domain)
                usecase.doesNotDependOn(adapter, framework)
                adapter.dependsOn(domain, usecase)
                adapter.doesNotDependOn(framework)
                framework.dependsOn(domain, usecase, adapter)
            }
    }

    @Test
    fun `domain層はSpringやJPAのフレームワークに依存していないこと`() {
        Konsist
            .scopeFromPackage("com.example.budgetapp.domain..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("org.springframework.") ||
                        import.name.startsWith("jakarta.persistence.")
                }
            }
    }

    @Test
    fun `usecase層はJPAに依存していないこと`() {
        Konsist
            .scopeFromPackage("com.example.budgetapp.usecase..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("jakarta.persistence.")
                }
            }
    }

    @Test
    fun `Controller層はOutputPortとDBアダプターに直接依存していないこと`() {
        Konsist
            .scopeFromPackage("com.example.budgetapp.adapter.controller..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.example.budgetapp.usecase.output.") ||
                        import.name.startsWith("com.example.budgetapp.adapter.repository.")
                }
            }
    }
}
