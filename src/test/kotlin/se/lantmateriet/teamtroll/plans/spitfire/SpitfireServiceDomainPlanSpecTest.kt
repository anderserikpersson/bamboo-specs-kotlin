package se.lantmateriet.teamtroll.plans.spitfire

import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException
import com.atlassian.bamboo.specs.api.util.EntityPropertiesBuilders
import org.junit.Test

class SpitfireServiceDomainPlanSpecTest {
    @Test
    @Throws(PropertiesValidationException::class)
    fun checkYourPlanOffline() {
        val plan = SpitfireServiceDomainPlanSpec().createPlan()
        EntityPropertiesBuilders.build(plan)
    }
}