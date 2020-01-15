package se.lantmateriet.teamtroll.plans

import com.atlassian.bamboo.specs.api.builders.docker.DockerConfiguration
import com.atlassian.bamboo.specs.api.builders.notification.Notification
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType
import com.atlassian.bamboo.specs.api.builders.permission.Permissions
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions
import com.atlassian.bamboo.specs.api.builders.plan.Job
import com.atlassian.bamboo.specs.api.builders.plan.Plan
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier
import com.atlassian.bamboo.specs.api.builders.plan.Stage
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact
import com.atlassian.bamboo.specs.api.builders.plan.branches.BranchCleanup
import com.atlassian.bamboo.specs.api.builders.plan.branches.PlanBranchManagement
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds
import com.atlassian.bamboo.specs.api.builders.plan.configuration.PluginConfiguration
import com.atlassian.bamboo.specs.api.builders.project.Project
import com.atlassian.bamboo.specs.api.builders.task.Task
import com.atlassian.bamboo.specs.api.builders.trigger.Trigger
import com.atlassian.bamboo.specs.builders.notification.CommittersRecipient
import com.atlassian.bamboo.specs.builders.notification.PlanFailedNotification
import com.atlassian.bamboo.specs.builders.task.CheckoutItem
import com.atlassian.bamboo.specs.builders.task.MavenTask
import com.atlassian.bamboo.specs.builders.task.ScriptTask
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask
import com.atlassian.bamboo.specs.builders.trigger.BitbucketServerTrigger
import com.atlassian.bamboo.specs.util.BambooServer
import com.google.common.base.Strings.isNullOrEmpty

abstract class AbstractPlanSpec {
    protected fun publish() { //By default credentials are read from the '.credentials' file.
        val bambooServer = BambooServer(BAMBOO_SERVER)
        val plan = createPlan()
        bambooServer.publish(plan)
        val planPermission = createPlanPermission(plan.identifier)
        bambooServer.publish(planPermission)
    }

    protected abstract fun createPlan(): Plan
    protected fun createBuildAndTestJob(tasks: Array<Task<*, *>>): Job {
        val job = job("Build and test", "JOB1")
        job.tasks(*tasks)
        job.dockerConfiguration(defaultDockerConfiguration())
        return job
    }

    protected fun defaultStage(jobs: Array<Job>): Stage {
        return stage("Stage 1", jobs)
    }

    protected fun createPlanPermission(identifier: PlanIdentifier): PlanPermissions {
        val permission = Permissions()
                .userPermissions(TEAMUSER_APP_ID, PermissionType.ADMIN, PermissionType.CLONE, PermissionType.EDIT)
                .groupPermissions(TEAMGROUP_ID, PermissionType.ADMIN)
                .loggedInUserPermissions(PermissionType.VIEW)
                .anonymousUserPermissionView()
        return PlanPermissions(identifier.projectKey, identifier.planKey).permissions(permission)
    }

    protected fun project(projectName: String, projectKey: String): Project {
        return Project()
                .name(projectName)
                .key(projectKey)
    }

    protected fun plan(name: String, key: String, description: String, project: Project, stages: Array<Stage>, linkedRepos: Array<String>): Plan {
        return Plan(project, name, key)
                .enabled(true)
                .description(description)
                .linkedRepositories(*linkedRepos)
                .stages(*stages)
                .pluginConfigurations(defaultPluginConfiguration())
                .triggers(defaultTrigger())
                .planBranchManagement(defaultPlanBranchManagement())
                .notifications(defaultNotification())
    }

    protected fun stage(name: String, jobs: Array<Job>): Stage {
        return Stage(name).jobs(*jobs)
    }

    protected fun job(name: String, key: String): Job {
        return Job(name, key)
    }

    protected fun defaultTrigger(): Trigger<*, *> {
        return BitbucketServerTrigger()
    }

    protected fun defaultPlanBranchManagement(): PlanBranchManagement {
        return PlanBranchManagement()
                .delete(BranchCleanup())
                .notificationForCommitters()
    }

    protected fun defaultPluginConfiguration(): PluginConfiguration<*> {
        return ConcurrentBuilds()
    }

    protected fun defaultNotification(): Notification {
        return Notification()
                .type(PlanFailedNotification())
                .recipients(CommittersRecipient())
    }

    protected fun defaultDockerConfiguration(): DockerConfiguration {
        return DockerConfiguration().enabled(false)
    }

    protected fun artifact(name: String, copyPattern: String): Artifact {
        return Artifact(name)
                .copyPattern(copyPattern)
                .shared(true)
                .required(true)
    }

    // Tasks
    protected fun checkoutDefaultRepoTask(): Task<*, *> {
        return VcsCheckoutTask()
                .description("Checkout Default Repository")
                .checkoutItems(CheckoutItem().defaultRepository())
                .cleanCheckout(true)
    }

    protected fun scriptTask(description: String, body: String): Task<*, *> {
        return ScriptTask()
                .description(description)
                .inlineBody(body)
    }

    protected fun mavenTask(description: String, goal: String): MavenTask {
        return MavenTask()
                .description(description)
                .goal(goal)
                .jdk(JDK_1_8)
                .executableLabel(EXECUTABLE_LABEL_MAVEN_3)
    }

    protected fun changeVersionNrInPomTask(): Task<*, *> {
        return mavenTask(
                "Change version",
                "versions:set -DnewVersion=\${bamboo.buildNumber}-\${bamboo.planRepository.branchName}")
    }

    protected fun jaCoCoTask(springpProfile: String = "default"): Task<*, *> {
        var goal = "clean org.jacoco:jacoco-maven-plugin:prepare-agent install" +
                " -Dmaven.test.failure.ignore=true" +
                " -Dsonar.jacoco.reportMissing.force.zero=true" +
                " -Dapplication.version=\${bamboo.buildNumber}" +
                " -Dspring.profiles.default=$springpProfile"
        val task = mavenTask("JaCoCo", goal)
        task.hasTests(true)
        return task
    }

    protected fun sonarQubeTask(springProfile: String = "default"): Task<*, *> {
        var goal = "sonar:sonar -Psonarqube -Dspring.profiles.default=$springProfile"
        val task = mavenTask("SonarQube", goal)
        task.hasTests(true)
        return task
    }

    protected fun deployToArtifactoryTask(springProfile: String = "default"): Task<*, *> {
        var goal = "deploy -Dapplication.version=\${bamboo.buildNumber}-\${bamboo.planRepository.branchName} -Dspring.profiles.default=$springProfile"
        return mavenTask("Deploy to artifactory", goal)
    }

    protected fun buildAndPushDockerImageTask(dockerImage: String): Task<*, *> {
        val body = "docker build -t " + dockerImage + ":\${bamboo.buildNumber}-\${bamboo.planRepository.branchName} .\n" +
                "docker login -u " + TEAMUSER_APP_ID + " -p \${bamboo.team_password} docker.domain.org\n" +
                "docker push " + dockerImage + ":\${bamboo.buildNumber}-\${bamboo.planRepository.branchName}\n" +
                "docker rmi " + dockerImage + ":\${bamboo.buildNumber}-\${bamboo.planRepository.branchName}"
        return scriptTask("Build and push docker image", body)
    }

    protected fun createFileWithBuildNumberTask(variableName: String, artifactName: String): Task<*, *> {
        val body = "echo $variableName=\${bamboo.buildNumber}-\${bamboo.planRepository.branchName} > $artifactName"
        return scriptTask("Create file with build number", body)
    }

    companion object {
        private const val JDK_1_8 = "JDK 1.8"
        private const val EXECUTABLE_LABEL_MAVEN_3 = "Maven 3.6.3"
        private const val BAMBOO_SERVER = "http://myserver"
        private const val TEAMUSER_APP_ID = "my_team_app_id"
        private const val TEAMGROUP_ID = "my_teamgroup_id"
    }
}