package com.msd.command.application

import com.msd.command.application.command.*
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
            "block $randomUUID  $randomUUID",
            "movE $randomUUID  $randomUUID $randomUUID",
            "fight $randomUUID  $randomUUID $randomUUID",
            "MINE $randomUUID  $randomUUID",
            "regenerate $randomUUID  $randomUUID",
            "Use-Item-Fighting $randomUUID Rocket $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID long_range_bombardment $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID self_destruction $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID NuKe $randomUUID  $randomUUID",
            "use-item-movement $randomUUID wormhole $randomUUID",
            "use-item-repair $randomUUID REPAIR_SWARM $randomUUID"
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
                assert(commands.count { it is FightingCommand } == 1)
            },
            {
                assert(commands.count { it is MineCommand } == 1)
            },
            {
                assert(commands.count { it is EnergyRegenCommand } == 1)
            },
            {
                assert(commands.count { it is FightingItemUsageCommand } == 4) // 4 !!!
            },
            {
                assert(commands.count { it is MovementItemsUsageCommand } == 1)
            },
            {
                assert(commands.count { it is RepairItemUsageCommand } == 1)
            }
        )
    }

    @Test
    fun `Throws CommandParsingException when command cannot be parsed`() {
        // given
        val missingUUID = "block $randomUUID"
        val unknownCommandType = "black $randomUUID $randomUUID"
        val tooManyUUIDs = "regenerate $randomUUID $randomUUID $randomUUID"
        val unknownMovementItemType = "use-item-movement $randomUUID sheep $randomUUID"
        val unknownRepairItemType = "use-item-repair $randomUUID sheep $randomUUID"
        val unknownFightingItemType = "use-item-fighting $randomUUID $randomUUID sheep $randomUUID $randomUUID"
        val invalidUUID = "use-item-fighting 1235 rocket $randomUUID $randomUUID"

        // then
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(missingUUID) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownCommandType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(tooManyUUIDs) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownMovementItemType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownRepairItemType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(unknownFightingItemType) }
        assertThrows<CommandParsingException> { commandApplicationService.parseCommand(invalidUUID) }
    }

    @Test
    fun `Rejects heterogeneous Attack and AttackItemUsage Commands`() {
        // given
        val attackCommandAndMineCommand = arrayListOf(
            "fight $randomUUID  $randomUUID $randomUUID",
            "MINE $randomUUID  $randomUUID"
        )

        val attackItemUsageCommandAndMovementItemUsageCommand = arrayListOf(
            "Use-Item-Fighting $randomUUID NuKe $randomUUID  $randomUUID",
            "use-item-movement $randomUUID wormhole $randomUUID",
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
