package com.msd.command.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.*

class CommandApplicationServiceTest {

    private val commandApplicationService = CommandApplicationService()

    private val randomUUID: UUID = UUID.randomUUID()

    @Test
    fun `Parses all valid command types`() {
        // given
        // intended weird upper and lowercase styles and double whitespaces, give the caller some slack
        val commandStrings = arrayListOf(
            "block $randomUUID  $randomUUID  $randomUUID",
            "movE $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "fight $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "MINE $randomUUID  $randomUUID  $randomUUID",
            "regenerate $randomUUID  $randomUUID  $randomUUID",
            "Use-Item-Fighting $randomUUID  $randomUUID Rocket $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID bombardment $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID self_destruct $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID NuKe $randomUUID  $randomUUID",
            "use-item-movement $randomUUID  $randomUUID wormhole $randomUUID",
            "use-item-reparation $randomUUID  $randomUUID REPARATION_SWARM $randomUUID"
        )

        // when
        val commands = commandStrings.map { commandApplicationService.parseCommand(it) }

        // then
        assertAll(
            {
                assert(commands.count { it is BlockCommand } == 1)
            },
            {
                assert(commands.count { it is MovementCommand } == 1)
            },
            {
                assert(commands.count { it is AttackCommand } == 1)
            },
            {
                assert(commands.count { it is MiningCommand } == 1)
            },
            {
                assert(commands.count { it is EnergyRegenCommand } == 1)
            },
            {
                assert(commands.count { it is AttackItemUsageCommand } == 4) // 4 !!!
            },
            {
                assert(commands.count { it is MovementItemsUsageCommand } == 1)
            },
            {
                assert(commands.count { it is ReparationItemUsageCommand } == 1)
            }
        )
    }

    @Test
    fun `Throws CommandParsingException when command cannot be parsed`() {
        // given
        val missingUUID = "block $randomUUID $randomUUID"
        val unknownCommandType = "black $randomUUID $randomUUID $randomUUID"
        val tooManyUUIDs = "regenerate $randomUUID $randomUUID $randomUUID $randomUUID"
        val unknownMovementItemType = "use-item-movement $randomUUID $randomUUID sheep $randomUUID"
        val unknownReparationItemType = "use-item-reparation $randomUUID $randomUUID sheep $randomUUID"
        val unknownFightingItemType = "use-item-fighting $randomUUID $randomUUID sheep $randomUUID $randomUUID"
        val invalidUUID = "use-item-fighting 1235 $randomUUID rocket $randomUUID $randomUUID"

        // then
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(missingUUID) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownCommandType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(tooManyUUIDs) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownMovementItemType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownReparationItemType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownFightingItemType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(invalidUUID) }
    }

    @Test
    fun `Rejects heterogeneous Attack and AttackItemUsage Commands`() {
        // given
        val attackCommandAndMineCommand = arrayListOf(
            "fight $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "MINE $randomUUID  $randomUUID  $randomUUID"
        )

        val attackItemUsageCommandAndMovementItemUsageCommand = arrayListOf(
            "Use-Item-Fighting $randomUUID  $randomUUID NuKe $randomUUID  $randomUUID",
            "use-item-movement $randomUUID  $randomUUID wormhole $randomUUID",
        )

        // then
        assertThrows<CommandBatchParsingException> {
            commandApplicationService.parseCommandsFromStrings(attackCommandAndMineCommand)
        }
        assertThrows<CommandBatchParsingException> {
            commandApplicationService.parseCommandsFromStrings(attackItemUsageCommandAndMovementItemUsageCommand)
        }
    }
}
