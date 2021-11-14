package com.msd.command.application

import com.msd.command.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.*

class CommandApplicationServiceTest {

    private val commandApplicationService = CommandApplicationService()

    private val randomUUID: UUID = UUID.randomUUID()

    @Test
    fun `Parses all valid command types`() {
        // intended weird upper and lowercase styles
        val commandStrings = arrayListOf(
            "block $randomUUID  $randomUUID  $randomUUID",
            "movE $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "fight $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "MINE $randomUUID  $randomUUID  $randomUUID",
            "regenerate $randomUUID  $randomUUID  $randomUUID",
            "Use-Item-Fighting $randomUUID  $randomUUID Rocket $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID bombardEment $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID self_destruct $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID NuKe $randomUUID  $randomUUID",
            "use-item-movement $randomUUID  $randomUUID wormhole $randomUUID",
            "use-item-healing $randomUUID  $randomUUID REPARATION_SWARM $randomUUID"
        )

        val commands = commandApplicationService.parseCommandsFromStrings(commandStrings)

        assertAll(
            {
                assert(commands.filter { it::class == BlockCommand::class }.count() == 1)
            },
            {
                assert(commands.filter { it::class == MovementCommand::class }.count() == 1)
            },
            {
                assert(commands.filter { it::class == AttackCommand::class }.count() == 1)
            },
            {
                assert(commands.filter { it::class == MiningCommand::class }.count() == 1)
            },
            {
                assert(commands.filter { it::class == EnergyRegenCommand::class }.count() == 1)
            },
            {
                assert(commands.filter { it::class == AttackItemUsageCommand::class }.count() == 4)
            },
            {
                assert(commands.filter { it::class == MovementItemsUsageCommand::class }.count() == 1)
            },
            {
                assert(commands.filter { it::class == ReparationItemUsageCommand::class }.count() == 1)
            }
        )
    }
}
