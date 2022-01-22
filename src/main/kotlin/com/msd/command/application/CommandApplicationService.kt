package com.msd.command.application

import com.msd.command.application.command.*
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.util.*

@Service
class CommandApplicationService {

    private val logger = KotlinLogging.logger {}

    val commandTypeKeywordsAndNumArgs = mapOf(
        CommandVerbs.BLOCK.verb to 2,
        CommandVerbs.MOVE.verb to 3,
        CommandVerbs.FIGHT.verb to 3,
        CommandVerbs.MINE.verb to 2,
        CommandVerbs.REGENERATE.verb to 2,
        CommandVerbs.USE_ITEM_FIGHTING.verb to 4,
        CommandVerbs.USE_ITEM_MOVEMENT.verb to 3,
        CommandVerbs.USE_ITEM_REPAIR.verb to 3
    )

    /**
     * Parses the list of commands and throws an Exception, if a homogeneity requirement isn't satisfied.
     *
     * @param commandStrings:       List of strings representing commands
     * @returns List of parsed commands
     */
    fun parseCommandsFromStrings(commandStrings: List<String>): List<Command> {
        val commands = commandStrings.map { parseCommand(it) }
        if (commands.filter { it::class == commands[0]::class }.count() != commands.size) {
            logger.info(
                "Rejected command batch because of inhomogeneity:\n Rejected transactionUUIDs:\n" +
                    "${commands.map{it.transactionUUID}}"
            )
            throw CommandBatchParsingException("Command batches need to be homogeneous.")
        }
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

        return parseCommandByVerb(verb, args, commandString)
    }

    /**
     * Parses a commandString by checking whether it is an item usage command or a action command.
     */
    private fun parseCommandByVerb(
        verb: String,
        args: List<String>,
        commandString: String
    ): Command {
        try {
            return if (verb.startsWith("use-item-"))
                parseItemUsageCommand(verb, args)
            else
                parseActionCommand(verb, args)
        } catch (iae: IllegalArgumentException) {
            if (iae.message?.contains("Invalid UUID") == true) {
                logger.info("Rejected command batch because of malformed UUID: ${iae.message}")
                throw CommandParsingException(commandString, "Invalid UUID string")
            } else {
                logger.info("Rejected command batch because of unknown ItemType: ${args[2]}")
                throw CommandParsingException(commandString, "Unknown ItemType: ${args[2]}")
            }
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
            CommandVerbs.USE_ITEM_FIGHTING.verb -> return FightingItemUsageCommand(
                UUID.fromString(args[0]),
                AttackItemType.valueOf(args[1].uppercase()),
                UUID.fromString(args[2]),
                UUID.fromString(args[3])
            )
            CommandVerbs.USE_ITEM_MOVEMENT.verb -> return MovementItemsUsageCommand(
                UUID.fromString(args[0]),
                MovementItemType.valueOf(args[1].uppercase()),
                UUID.fromString(args[2])
            )
            CommandVerbs.USE_ITEM_REPAIR.verb -> return RepairItemUsageCommand(
                UUID.fromString(args[0]),
                RepairItemType.valueOf(args[1].uppercase()),
                UUID.fromString(args[2])
            )
            else -> throw CommandParsingException("Unknown command verb $verb")
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
            CommandVerbs.BLOCK.verb,
            CommandVerbs.MINE.verb,
            CommandVerbs.REGENERATE.verb -> get2PartConstructorByVerb(verb)(
                UUID.fromString(args[0]),
                UUID.fromString(args[1])
            )
            CommandVerbs.MOVE.verb, CommandVerbs.FIGHT.verb -> get3PartConstructorByVerb(verb)(
                UUID.fromString(args[0]),
                UUID.fromString(args[1]),
                UUID.fromString(args[2])
            )
            else -> throw CommandParsingException("Unknown Command verb $verb")
        }
    }

    /**
     * Get the constructor for the command of the verb-type
     *
     * @param verb  the verb corresponding with a specific command type
     * @return The Constructor for the Command
     */
    fun get2PartConstructorByVerb(verb: String): (UUID, UUID) -> Command {
        return when (verb) {
            CommandVerbs.BLOCK.verb -> ::BlockCommand
            CommandVerbs.MINE.verb -> ::MineCommand
            CommandVerbs.REGENERATE.verb -> ::EnergyRegenCommand
            else -> throw CommandParsingException("Unknown Command verb $verb")
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
            CommandVerbs.MOVE.verb -> ::MovementCommand
            CommandVerbs.FIGHT.verb -> ::FightingCommand
            else -> throw CommandParsingException("Unknown Command verb $verb")
        }
    }

    fun executeAsync(commands: List<Command>) {
        val thread = Thread(AsyncCommandExecutor())
    }
}
