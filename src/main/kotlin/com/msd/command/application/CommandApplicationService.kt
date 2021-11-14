package com.msd.command.application

import com.msd.command.domain.*
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.ReparationItemType
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.util.*

@Service
class CommandApplicationService {

    val commandTypeKeywordsAndNumArgs = mapOf(
        "block" to 3,
        "move" to 4,
        "fight" to 4,
        "mine" to 3,
        "regenerate" to 3,
        "use-item-fighting" to 5,
        "use-item-movement" to 4,
        "use-item-healing" to 4
    )

    fun parseCommandsFromStrings(commandStrings: List<String>): List<Command> {
        val commands = commandStrings.map { parseCommand(it) }
        if (commands.find { it::class == AttackCommand::class } != null)
            if (commands.filter { it::class == AttackCommand::class }.count() != commands.size)
                throw CommandBatchParsingException("AttackCommands need to be homogeneous.")
        if (commands.find { it::class == AttackItemUsageCommand::class } != null)
            if (commands.filter { it::class == AttackItemUsageCommand::class }.count() != commands.size)
                throw CommandBatchParsingException("AttackItemUsageCommand need to be homogeneous.")
        return commands
    }

    fun parseCommand(commandString: String): Command {
        val (verb, args) = getVerbAndArgs(commandString)

        if (verb !in commandTypeKeywordsAndNumArgs.keys) throw CommandParsingException(commandString)

        if (args.count() != commandTypeKeywordsAndNumArgs[verb]) throw CommandParsingException(commandString)

        try {
            return if (verb.startsWith("use-item-"))
                parseItemUsageCommand(verb, args)
            else
                parseActionCommand(verb, args)
        } catch (iae: IllegalArgumentException) {
            if (iae.message?.contains("Invalid UUID") == true)
                throw CommandParsingException(commandString, "Invalid UUID string")
            else throw CommandParsingException(commandString, "Unknown ItemType: ${args[2]}")
        }
    }

    private fun getVerbAndArgs(commandString: String): Pair<String, List<String>> {
        val parts = commandString
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .filter { it.isNotBlank() }
        val verb = parts.first()
        val args = parts.takeLast(parts.size - 1)
        return Pair(verb, args)
    }

    private fun parseItemUsageCommand(verb: String, args: List<String>): Command {
        when (verb) {
            "use-item-fighting" -> return AttackItemUsageCommand(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                AttackItemType.valueOf(args[2].uppercase()),
                UUID.fromString(args[3]),
                UUID.fromString(args[4])
            )
            "use-item-movement" -> return MovementItemsUsageCommand(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                MovementItemType.valueOf(args[2].uppercase()),
                UUID.fromString(args[3])
            )
            "use-item-healing" -> return ReparationItemUsageCommand(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                ReparationItemType.valueOf(args[2].uppercase()),
                UUID.fromString(args[3])
            )
        }
        throw RuntimeException("Internal Error")
    }

    private fun parseActionCommand(verb: String, args: List<String>): Command {
        when (verb) {
            "block", "mine", "regenerate" -> return get3PartConstructorByVerb(verb)(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                UUID.fromString(args[2])
            )
            "move", "fight" -> return get4PartConstructorByVerb(verb)(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                UUID.fromString(args[2]),
                UUID.fromString(args[3])
            )
        }
        throw RuntimeException("Internal Error")
    }

    fun get3PartConstructorByVerb(verb: String): (UUID, UUID, UUID) -> Command {
        when (verb) {
            "block" -> return ::BlockCommand
            "mine" -> return ::MiningCommand
            "regenerate" -> return ::EnergyRegenCommand
        }
        throw RuntimeException("Internal Error")
    }

    fun get4PartConstructorByVerb(verb: String): (UUID, UUID, UUID, UUID) -> Command {
        when (verb) {
            "move" -> return ::MovementCommand
            "fight" -> return ::AttackCommand
        }
        throw RuntimeException("Internal Error")
    }
}
