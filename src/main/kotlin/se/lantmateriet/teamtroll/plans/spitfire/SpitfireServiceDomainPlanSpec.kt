package se.lantmateriet.teamtroll.plans.spitfire

import com.atlassian.bamboo.specs.api.BambooSpec
import com.atlassian.bamboo.specs.api.builders.plan.Plan
import com.atlassian.bamboo.specs.api.builders.plan.Stage
import com.atlassian.bamboo.specs.api.builders.task.Task
import se.lantmateriet.teamtroll.plans.AbstractPlanSpec

/**
 * Plan configuration for Bamboo.
 * Learn more on: [https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs](https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs)
 */
@BambooSpec
class SpitfireServiceDomainPlanSpec : AbstractPlanSpec() {
    public override fun createPlan(): Plan {
        val project = project("Spitfire", "SPIT")
        project.description("Service for Spitfire")
        val job = createBuildAndTestJob(buildTasks())
        val stages: Array<Stage> = arrayOf(defaultStage(arrayOf(job)))
        val linkedRepos: Array<String> = arrayOf("Git Spitfire Domain")
        return plan(
                "Spitfire Service - Domain",
                "SSD",
                "See https://git/projects/TT/repos/bamboo-build/browse",
                project,
                stages,
                linkedRepos)
    }

    private fun buildTasks(): Array<Task<*, *>> {
        return arrayOf(
                checkoutDefaultRepoTask(),
                changeVersionNrInPomTask(),
                jaCoCoTask(),
                sonarQubeTask(),
                deployToArtifactoryTask()
        )
    }

    companion object {
        /**
         * Run main to publish plan on Bamboo
         */
        @JvmStatic
        fun main(args: Array<String>) {
            SpitfireServiceDomainPlanSpec().publish()
        }
    }
}