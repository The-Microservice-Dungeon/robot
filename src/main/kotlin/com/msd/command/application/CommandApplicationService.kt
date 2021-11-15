package com.msd.command.application

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
        "use-item-reparation" to 4
    )

    /**
     * Parses the list of commands and thrwos an Exception, if a homogeneity requirement isn't satisfied.
     *
     * @param commandStrings:       List of strings representing commands
     * @returns List of parsed commands
     */
    fun parseCommandsFromStrings(commandStrings: List<String>): List<Command> {
        val commands = commandStrings.map { parseCommand(it) }
        if (commands.find { it is AttackCommand } != null)
            if (commands.filterIsInstance<AttackCommand>().count() != commands.size)
                throw CommandBatchParsingException("AttackCommands need to be homogeneous.")
        if (commands.find { it is AttackItemUsageCommand } != null)
            if (commands.filterIsInstance<AttackItemUsageCommand>().count() != commands.size)
                throw CommandBatchParsingException("AttackItemUsageCommand need to be homogeneous.")
        return commands
    }

    /**
     * Parse a single commandString into a Command object.
     *
     * @throws CommandParsingException if there are not enough arguments for the command type or if the command type
     *         is unknown
     * @param commandString The string representation of a command
     * @return The corresponding Command object
     */
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

    /**
     * Parse the command string into the verb and the remaining arguments.
     *
     * @param commandString: The string containing a command
     * @return Pair of verb and args
     */
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

    /**
     * Parse all commands describing the usage of an item
     *
     * @param verb  The command verb, describing the type of command
     * @param args  The arguments for the command
     */
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
            "use-item-reparation" -> return ReparationItemUsageCommand(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                ReparationItemType.valueOf(args[2].uppercase()),
                UUID.fromString(args[3])
            )
            else -> throw RuntimeException("Internal Error")
        }
    }

    /**
     * Parse all commands that are not an item usage
     *
     * @param verb  The command verb, describing the type of command
     * @param args  The arguments for the command
     */
    private fun parseActionCommand(verb: String, args: List<String>): Command {
        return when (verb) {
            "block", "mine", "regenerate" -> get3PartConstructorByVerb(verb)(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                UUID.fromString(args[2])
            )
            "move", "fight" -> get4PartConstructorByVerb(verb)(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                UUID.fromString(args[2]),
                UUID.fromString(args[3])
            )
            else -> throw RuntimeException("Internal Error")
        }
    }

    /**
     * Get the constructor for the command of the verb-type
     *
     * @param verb  the verb corresponding with a specific command type
     * @return The Constructor for the Command
     */
    fun get3PartConstructorByVerb(verb: String): (UUID, UUID, UUID) -> Command {
        return when (verb) {
            "block" -> ::BlockCommand
            "mine" -> ::MiningCommand
            "regenerate" -> ::EnergyRegenCommand
            else -> throw RuntimeException("Internal Error")
        }
    }

    /**
     * Get the constructor for the command of the verb-type
     *
     * @param verb  the verb corresponding with a specific command type
     * @return The Constructor for the Command
     */
    fun get4PartConstructorByVerb(verb: String): (UUID, UUID, UUID, UUID) -> Command {
        return when (verb) {
            "move" -> ::MovementCommand
            "fight" -> ::AttackCommand
            else -> throw RuntimeException("Internal Error")
        }
    }
}
