package et.frectonz.kimem.core.network

import android.util.Log
import et.frectonz.kimem.R
import et.frectonz.kimem.core.codec.RouterDateTime
import et.frectonz.kimem.core.codec.b64Encode
import et.frectonz.kimem.core.codec.joinMsgIds
import et.frectonz.kimem.core.codec.sha256Hex
import et.frectonz.kimem.core.codec.ucs2Encode
import et.frectonz.kimem.core.model.ActionKind
import et.frectonz.kimem.core.model.ActionReply
import et.frectonz.kimem.core.model.ActionResult
import et.frectonz.kimem.core.model.AirtimeBalance
import et.frectonz.kimem.core.model.ApnConfig
import et.frectonz.kimem.core.model.ApnReport
import et.frectonz.kimem.core.model.ApnStatus
import et.frectonz.kimem.core.model.CellExtras
import et.frectonz.kimem.core.model.Device
import et.frectonz.kimem.core.model.DeviceReport
import et.frectonz.kimem.core.model.GetRandomLogin
import et.frectonz.kimem.core.model.Info
import et.frectonz.kimem.core.model.Internet
import et.frectonz.kimem.core.model.InternetReport
import et.frectonz.kimem.core.model.LoginReply
import et.frectonz.kimem.core.model.LoginResult
import et.frectonz.kimem.core.model.Message
import et.frectonz.kimem.core.model.MessageStatus
import et.frectonz.kimem.core.model.Power
import et.frectonz.kimem.core.model.RouterException
import et.frectonz.kimem.core.model.Signal
import et.frectonz.kimem.core.model.SignalReport
import et.frectonz.kimem.core.model.SimIccid
import et.frectonz.kimem.core.model.SmsCapacity
import et.frectonz.kimem.core.model.SmsInbox
import et.frectonz.kimem.core.model.SmsParameters
import et.frectonz.kimem.core.model.SmsSettings
import et.frectonz.kimem.core.model.StationList
import et.frectonz.kimem.core.model.SystemInfo
import et.frectonz.kimem.core.model.UssdData
import et.frectonz.kimem.core.model.UssdFlag
import et.frectonz.kimem.core.model.UssdWriteFlag
import et.frectonz.kimem.core.model.WanDetails
import et.frectonz.kimem.core.model.Wifi
import et.frectonz.kimem.core.model.res
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "Kimem"

suspend fun <T> withRetry(action: suspend () -> T): T {
    val maxAttempts = 3
    val baseDelayMs = 200L
    for (attempt in 1..maxAttempts) {
        try {
            return action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (attempt == maxAttempts) throw e
            val wait = baseDelayMs shl (attempt - 1)
            Log.d(TAG, "Retrying in ${wait}ms (${e.message})")
            delay(wait)
        }
    }
    error("unreachable")
}

class RouterClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    fun open(host: String, username: String, password: String): Router = Router(http, host, username, password)
}

class Router(
    private val http: OkHttpClient,
    host: String,
    private val username: String,
    private val password: String,
) {
    private val address = "http://${host.trim()}"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { it.body.string() }
        } catch (e: IOException) {
            throw RouterException(res(R.string.err_request_failed, e.message ?: e.javaClass.simpleName), e)
        }
    }

    private fun <T> decode(serializer: KSerializer<T>, text: String): T {
        val element = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw RouterException(res(R.string.err_non_json, text), e)
        }
        return try {
            json.decodeFromJsonElement(serializer, element)
        } catch (e: SerializationException) {
            throw RouterException(res(R.string.err_decode, text), e)
        } catch (e: IllegalArgumentException) {
            throw RouterException(res(R.string.err_decode, text), e)
        }
    }

    private suspend fun fetch(url: String): String = withRetry {
        execute(Request.Builder().url(url).header("User-Agent", "Kimem Android").build())
    }

    private suspend fun getRaw(cmd: String, params: Map<String, String>): String {
        val url = "$address/reqproc/proc_get".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("isTest", "false")
            .addQueryParameter("cmd", cmd)
            .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()
        return fetch(url.toString())
    }

    private suspend inline fun <reified T> get(cmd: String, params: Map<String, String> = emptyMap()): T =
        decode(serializer<T>(), getRaw(cmd, params))

    private suspend inline fun <reified T> getMulti(cmds: List<String>): T =
        decode(serializer<T>(), fetch("$address/reqproc/proc_get?cmd=${cmds.joinToString(",")}&multi_data=1&isTest=false"))

    private suspend fun postRaw(goformId: String, params: Map<String, String> = emptyMap()): String {
        val form = FormBody.Builder()
            .add("isTest", "false")
            .add("goformId", goformId)
            .apply { params.forEach { (k, v) -> add(k, v) } }
            .build()
        val request = Request.Builder()
            .url("$address/reqproc/proc_post")
            .header("User-Agent", "Kimem Android")
            .header("Referer", address)
            .post(form)
            .build()
        return execute(request)
    }

    private suspend inline fun <reified T> post(goformId: String, params: Map<String, String> = emptyMap()): T =
        decode(serializer<T>(), withRetry { postRaw(goformId, params) })

    private suspend fun action(kind: ActionKind, goformId: String, params: Map<String, String>): ActionResult =
        ActionResult(kind, post<ActionReply>(goformId, params).result)

    suspend fun login() {
        val nonce = get<GetRandomLogin>("get_random_login").randomLogin
        val reply = post<LoginReply>(
            "LOGIN", mapOf(
                "username" to b64Encode(username),
                "password" to b64Encode(sha256Hex(nonce + password)),
                "unique_login_credentials" to "1",
            )
        )
        when (val result = reply.result) {
            LoginResult.Success -> Unit
            LoginResult.InvalidCredentials -> throw RouterException(res(R.string.err_login_wrong))
            LoginResult.MalformedRequest -> throw RouterException(res(R.string.err_login_malformed))
            is LoginResult.Other -> throw RouterException(res(R.string.err_login_code, result.code))
        }
    }

    suspend fun logout() {
        withRetry { postRaw("LOGOUT") }
    }

    suspend fun <T> session(block: suspend Router.() -> T): T {
        login()
        try {
            return block()
        } finally {
            withContext(NonCancellable) {
                try {
                    logout()
                } catch (e: Exception) {
                    Log.d(TAG, "logout failed: ${e.message}")
                }
            }
        }
    }

    suspend fun reboot() {
        try {
            postRaw("REBOOT_DEVICE")
        } catch (e: RouterException) {
            Log.d(TAG, "reboot response not read: ${e.message}")
        }
    }

    suspend fun info(): Info = getMulti(Info.CMDS)
    suspend fun system(): SystemInfo = getMulti(SystemInfo.CMDS)
    suspend fun wifi(): Wifi = getMulti(Wifi.CMDS)
    suspend fun power(): Power = getMulti(Power.CMDS)
    suspend fun devices(): StationList = get(StationList.CMD)
    suspend fun airtime(): AirtimeBalance = get(AirtimeBalance.CMD)

    suspend fun signal(): SignalReport = SignalReport(get(Signal.CMD), getMulti(CellExtras.CMDS))

    suspend fun internet(): InternetReport = InternetReport(getMulti(Internet.CMDS), get(WanDetails.CMD))

    suspend fun apn(): ApnReport =
        ApnReport(get<ApnStatus>(ApnStatus.CMD).apnMode, get<ApnConfig>(ApnConfig.CMD).profile)

    suspend fun device(): DeviceReport = DeviceReport(get(Device.CMD), get<SimIccid>(SimIccid.CMD).ziccid)

    suspend fun syslog(): String = fetch("$address/data/syslog.html?uniquelogincredentials=1&isTest=false")

    suspend fun smsInbox(): SmsInbox = get(SmsInbox.CMD, SmsInbox.PARAMS)

    suspend fun sms(id: Int): Message =
        smsInbox().messages.firstOrNull { it.id == id } ?: throw RouterException(res(R.string.err_no_message, id))

    suspend fun smsInfo(): SmsSettings = SmsSettings(get(SmsCapacity.CMD), get(SmsParameters.CMD))

    suspend fun sendSms(number: String, message: String): ActionResult = action(
        ActionKind.Send, "SEND_SMS", mapOf(
            "notCallback" to "true",
            "encode_type" to "UNICODE",
            "ID" to "-1",
            "Number" to number,
            "MessageBody" to ucs2Encode(message),
            "sms_time" to RouterDateTime.routerTimeNow(),
        )
    )

    suspend fun deleteSms(ids: Iterable<Int>): ActionResult =
        action(ActionKind.Delete, "DELETE_SMS", mapOf("msg_id" to joinMsgIds(ids)))

    suspend fun deleteAllSms(): ActionResult? {
        val messages = smsInbox().messages
        if (messages.isEmpty()) return null
        return deleteSms(messages.map { it.id })
    }

    suspend fun markSms(ids: Iterable<Int>): ActionResult =
        action(ActionKind.Mark, "SET_MSG_READ", mapOf("msg_id" to joinMsgIds(ids), "tag" to "0"))

    suspend fun markAllSms(): ActionResult? {
        val unread = smsInbox().messages.filter { it.tag is MessageStatus.Unread }.map { it.id }
        if (unread.isEmpty()) return null
        return markSms(unread)
    }

    suspend fun ussdSend(code: String) {
        post<ActionReply>("USSD_PROCESS", mapOf("USSD_operator" to "ussd_send", "USSD_send_number" to code, "notCallback" to "true"))
    }

    suspend fun ussdReply(reply: String) {
        post<ActionReply>("USSD_PROCESS", mapOf("USSD_operator" to "ussd_reply", "USSD_reply_number" to reply, "notCallback" to "true"))
    }

    suspend fun ussdCancel() {
        try {
            postRaw("USSD_PROCESS", mapOf("USSD_operator" to "ussd_cancel"))
        } catch (e: RouterException) {
            Log.d(TAG, "ussd cancel failed: ${e.message}")
        }
    }

    suspend fun ussdAwaitResponse(): String {
        val maxPolls = 30
        var polls = 0
        while (true) {
            delay(1_000)
            when (val flag = get<UssdWriteFlag>(UssdWriteFlag.CMD).flag) {
                UssdFlag.Ready -> break
                is UssdFlag.Failed -> throw RouterException(res(R.string.err_ussd_failed, flag.reason))
                UssdFlag.Pending -> {
                    polls += 1
                    if (polls == maxPolls) throw RouterException(res(R.string.err_ussd_timeout, maxPolls))
                }
            }
        }
        return get<UssdData>(UssdData.CMD).text.trim()
    }
}
