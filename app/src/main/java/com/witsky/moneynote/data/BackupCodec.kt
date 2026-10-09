package com.witsky.moneynote.data

import kotlinx.serialization.json.Json

/** 备份文件不可用。解析失败与版本不符都归到这一种异常，界面只需处理一种失败。 */
class BackupFormatException(message: String) : Exception(message)

/**
 * 备份文件的编解码。
 *
 * 解析失败很重要，但更重要的**是失败时不要写库**：所以这里只做纯解析，
 * 写入交给仓储，且写入在一个事务里完成。
 */
object BackupCodec {

  private val json = Json {
    // 新版本导出的文件里可能有旧版本不认识的字段，忽略即可，不要因此拒绝整个文件。
    ignoreUnknownKeys = true
    // 让备份文件在文本编辑器里可读：出问题时用户和我都能直接打开看。
    prettyPrint = true
    encodeDefaults = true
  }

  fun encode(payload: BackupPayload): String =
    json.encodeToString(BackupPayload.serializer(), payload)

  fun decode(text: String): BackupPayload {
    val payload = try {
      json.decodeFromString(BackupPayload.serializer(), text)
    } catch (e: Exception) {
      throw BackupFormatException("这个文件不是本应用导出的备份，或者内容已经损坏。")
    }
    if (payload.format != BackupPayload.CURRENT_FORMAT) {
      throw BackupFormatException("备份文件的格式版本不认识：${payload.format}")
    }
    return payload
  }
}
