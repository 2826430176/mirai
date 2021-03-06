/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console

import net.mamoe.mirai.console.plugins.PluginManager

object CommandManager {
    private val registeredCommand: MutableMap<String, ICommand> = mutableMapOf()

    fun getCommands(): Collection<ICommand> {
        return registeredCommand.values
    }


    fun register(command: ICommand) {
        val allNames = mutableListOf(command.name).also { it.addAll(command.alias) }
        allNames.forEach {
            if (registeredCommand.containsKey(it)) {
                error("net.mamoe.mirai.Command Name(or Alias) $it is already registered, consider if same function plugin was installed")
            }
        }
        allNames.forEach {
            registeredCommand[it] = command
        }
    }

    fun unregister(command: ICommand) {
        val allNames = mutableListOf<String>(command.name).also { it.addAll(command.alias) }
        allNames.forEach {
            registeredCommand.remove(it)
        }
    }

    fun unregister(commandName: String) {
        registeredCommand.remove(commandName)
    }

    suspend fun runCommand(fullCommand: String): Boolean {
        val blocks = fullCommand.split(" ")
        val commandHead = blocks[0].replace("/", "")
        if (!registeredCommand.containsKey(commandHead)) {
            return false
        }
        val args = blocks.subList(1, blocks.size)
        registeredCommand[commandHead]?.run {
            if (onCommand(
                    blocks.subList(1, blocks.size)
                )
            ) {
                PluginManager.onCommand(this, args)
            }
        }
        return true
    }

}

interface ICommand {
    val name: String
    val alias: List<String>
    val description: String
    suspend fun onCommand(args: List<String>): Boolean
    fun register()
}

abstract class Command(
    override val name: String,
    override val alias: List<String> = listOf(),
    override val description: String = ""
) : ICommand {
    /**
     * 最高优先级监听器
     * 如果 return `false` 这次指令不会被 [PluginBase] 的全局 onCommand 监听器监听
     * */
    open override suspend fun onCommand(args: List<String>): Boolean {
        return true
    }

    override fun register() {
        CommandManager.register(this)
    }
}

class AnonymousCommand internal constructor(
    override val name: String,
    override val alias: List<String>,
    override val description: String,
    val onCommand: suspend ICommand.(args: List<String>) -> Boolean
) : ICommand {
    override suspend fun onCommand(args: List<String>): Boolean {
        return onCommand.invoke(this, args)
    }

    override fun register() {
        CommandManager.register(this)
    }
}

class CommandBuilder internal constructor() {
    var name: String? = null
    var alias: List<String>? = null
    var description: String = ""
    var onCommand: (suspend ICommand.(args: List<String>) -> Boolean)? = null

    fun onCommand(commandProcess: suspend ICommand.(args: List<String>) -> Boolean) {
        onCommand = commandProcess
    }

    fun register(): ICommand {
        if (name == null || onCommand == null) {
            error("net.mamoe.mirai.CommandBuilder not complete")
        }
        if (alias == null) {
            alias = listOf()
        }
        return AnonymousCommand(name!!, alias!!, description, onCommand!!).also { it.register() }
    }
}

fun registerCommand(builder: CommandBuilder.() -> Unit): ICommand {
    return CommandBuilder().apply(builder).register()
}

