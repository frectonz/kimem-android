package et.frectonz.kimem.ui

import androidx.annotation.StringRes
import et.frectonz.kimem.R

sealed interface Node {
    val name: String
    val description: Int

    data class Menu(override val name: String, @StringRes override val description: Int, val children: List<Node>) : Node

    data class Leaf(
        override val name: String,
        @StringRes override val description: Int,
        val target: Route,
        val usage: String = "",
    ) : Node

    data class Action(override val name: String, @StringRes override val description: Int, val action: MenuAction) : Node
}

enum class MenuAction { Reboot }

private fun report(kind: ReportKind) = Node.Leaf(kind.name.lowercase(), kind.description, Route.Report(kind))

val COMMAND_TREE = Node.Menu(
    "kimem", R.string.app_subtitle, listOf(
        Node.Menu(
            "get", R.string.desc_get, listOf(
                report(ReportKind.Info),
                report(ReportKind.System),
                report(ReportKind.Signal),
                report(ReportKind.Internet),
                report(ReportKind.Apn),
                report(ReportKind.Device),
                report(ReportKind.Wifi),
                report(ReportKind.Devices),
                Node.Menu(
                    "sms", R.string.desc_get_sms, listOf(
                        Node.Leaf("list", R.string.desc_sms_list, Route.SmsInbox),
                        Node.Leaf("show", R.string.desc_sms_show, Route.SmsShow, usage = "<msg_id>"),
                        Node.Leaf("info", R.string.desc_sms_info, Route.Report(ReportKind.SmsInfo)),
                    )
                ),
                report(ReportKind.Syslog),
                report(ReportKind.Airtime),
                report(ReportKind.Power),
            )
        ),
        Node.Menu(
            "post", R.string.desc_post, listOf(
                Node.Action("reboot", R.string.desc_reboot, MenuAction.Reboot),
                Node.Leaf("ussd", R.string.desc_ussd, Route.Ussd(), usage = "<code>"),
                Node.Menu(
                    "sms", R.string.desc_post_sms, listOf(
                        Node.Leaf("send", R.string.desc_sms_send, Route.SendSms(), usage = "<number> <message>"),
                        Node.Leaf("delete", R.string.desc_sms_delete, Route.SmsSelect(SmsAction.Delete), usage = "<msg_id>|all"),
                        Node.Leaf("mark", R.string.desc_sms_mark, Route.SmsSelect(SmsAction.Mark), usage = "<msg_id>|all"),
                    )
                ),
            )
        ),
    )
)

fun resolveMenu(path: List<String>): Node.Menu? {
    var node: Node.Menu = COMMAND_TREE
    for (segment in path) {
        node = node.children.filterIsInstance<Node.Menu>().firstOrNull { it.name == segment } ?: return null
    }
    return node
}
