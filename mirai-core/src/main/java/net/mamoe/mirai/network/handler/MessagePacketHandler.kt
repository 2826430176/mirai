package net.mamoe.mirai.network.handler

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.QQ
import net.mamoe.mirai.event.events.group.GroupMessageEvent
import net.mamoe.mirai.event.events.qq.FriendMessageEvent
import net.mamoe.mirai.event.hookWhile
import net.mamoe.mirai.message.defaults.Image
import net.mamoe.mirai.message.defaults.MessageChain
import net.mamoe.mirai.message.defaults.PlainText
import net.mamoe.mirai.network.LoginSession
import net.mamoe.mirai.network.packet.ServerFriendMessageEventPacket
import net.mamoe.mirai.network.packet.ServerGroupMessageEventPacket
import net.mamoe.mirai.network.packet.ServerGroupUploadFileEventPacket
import net.mamoe.mirai.network.packet.ServerPacket
import net.mamoe.mirai.network.packet.action.ClientSendFriendMessagePacket
import net.mamoe.mirai.network.packet.action.ClientSendGroupMessagePacket
import net.mamoe.mirai.network.packet.action.ServerSendFriendMessageResponsePacket
import net.mamoe.mirai.network.packet.action.ServerSendGroupMessageResponsePacket

/**
 * 处理消息事件, 承担消息发送任务.
 *
 * @author Him188moe
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class MessagePacketHandler(session: LoginSession) : PacketHandler(session) {
    internal var ignoreMessage: Boolean = true

    init {
        //todo for test
        FriendMessageEvent::class.hookWhile {
            if (session.socket.isClosed()) {
                return@hookWhile false
            }
            when {
                it.message valueEquals "你好" -> it.sender.sendMessage("你好!")
                it.message.toString().startsWith("复读") -> it.sender.sendMessage(it.message())
                it.message.toString().startsWith("发群") -> {
                    it.message().list.toMutableList().let { messages ->
                        messages.removeAt(0)
                        sendGroupMessage(Group(session.bot, 580266363), MessageChain(messages))
                    }
                }
                /*it.message valueEquals "发图片群" -> sendGroupMessage(Group(session.bot, 580266363), PlainText("test") + UnsolvedImage(File("C:\\Users\\Him18\\Desktop\\faceImage_1559564477775.jpg")).also { image ->
                    image.upload(session, Group(session.bot, 580266363)).get()
                })*/
                it.message valueEquals "发图片群2" -> sendGroupMessage(Group(session.bot, 580266363), Image("{7AA4B3AA-8C3C-0F45-2D9B-7F302A0ACEAA}.jpg").toChain())
                /* it.message valueEquals "发图片" -> sendFriendMessage(it.sender, PlainText("test") + UnsolvedImage(File("C:\\Users\\Him18\\Desktop\\faceImage_1559564477775.jpg")).also { image ->
                     image.upload(session, it.sender).get()
                 })*/
                it.message valueEquals "发图片2" -> sendFriendMessage(it.sender, PlainText("test") + Image("{7AA4B3AA-8C3C-0F45-2D9B-7F302A0ACEAA}.jpg"))
            }

            return@hookWhile true
        }

        GroupMessageEvent::class.hookWhile {
            if (session.socket.isClosed()) {
                return@hookWhile false
            }

            when {
                it.message.contains("复读") -> it.group.sendMessage(it.chain)
            }

            return@hookWhile true

        }
    }

    override fun onPacketReceived(packet: ServerPacket) {
        when (packet) {
            is ServerGroupUploadFileEventPacket -> {
                //todo
            }

            is ServerFriendMessageEventPacket -> {
                if (ignoreMessage) return

                FriendMessageEvent(session.bot, session.bot.contacts.getQQ(packet.qq), packet.message).broadcast()
            }

            is ServerGroupMessageEventPacket -> {
                if (ignoreMessage) return

                if (packet.qq == session.bot.account.qqNumber) return

                GroupMessageEvent(session.bot, session.bot.contacts.getGroupByNumber(packet.groupNumber), session.bot.contacts.getQQ(packet.qq), packet.message).broadcast()
            }

            is ServerSendFriendMessageResponsePacket,
            is ServerSendGroupMessageResponsePacket -> {
                //ignored
            }
            else -> {
                //ignored
            }
        }
    }

    fun sendFriendMessage(qq: QQ, message: MessageChain) {
        session.socket.sendPacketAsync(ClientSendFriendMessagePacket(session.bot.account.qqNumber, qq.number, session.sessionKey, message))
    }

    fun sendGroupMessage(group: Group, message: MessageChain) {
        session.socket.sendPacket(ClientSendGroupMessagePacket(session.bot.account.qqNumber, group.groupId, session.sessionKey, message))
    }
}