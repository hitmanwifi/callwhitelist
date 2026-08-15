package org.alexrust.callwhitelist.data

import android.content.Context
import org.alexrust.callwhitelist.model.CallDecision
import org.alexrust.callwhitelist.model.FilterPolicyRule
import org.alexrust.callwhitelist.model.FilterProfile
import org.alexrust.callwhitelist.model.FilterSnapshot
import org.alexrust.callwhitelist.model.PolicyCondition
import org.alexrust.callwhitelist.model.PolicyMatchType
import org.alexrust.callwhitelist.model.TimeWindow
import org.json.JSONArray
import org.json.JSONObject

class FilterSnapshotStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "filter_snapshot",
        Context.MODE_PRIVATE,
    )

    fun read(): FilterSnapshot? {
        val raw = preferences.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching { JSONObject(raw).toSnapshot() }.getOrNull()
    }

    fun write(snapshot: FilterSnapshot): Boolean {
        return preferences.edit()
            .putString(KEY_SNAPSHOT, snapshot.toJson().toString())
            .commit()
    }

    private companion object {
        const val KEY_SNAPSHOT = "snapshot"
    }
}

private fun FilterSnapshot.toJson(): JSONObject = JSONObject().apply {
    put("version", version)
    put("contactsAllowed", contactsAllowed)
    put("emergencyNumbersAlwaysAllowed", emergencyNumbersAlwaysAllowed)
    put("profiles", JSONArray(profiles.map { it.toJson() }))
}

private fun FilterProfile.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("enabled", enabled)
    put("priority", priority)
    put("defaultDecision", defaultDecision.name)
    putNullable("activeWindow", activeWindow?.toJson())
    put("rules", JSONArray(rules.map { it.toJson() }))
}

private fun FilterPolicyRule.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("conditionType", condition.type.name)
    putNullable("conditionValue", condition.value)
    put("label", label)
    put("enabled", enabled)
    put("decision", decision.name)
    put("priority", priority)
    putNullable("timeWindow", timeWindow?.toJson())
}

private fun TimeWindow.toJson(): JSONObject = JSONObject().apply {
    put("daysOfWeek", JSONArray(daysOfWeek.toList()))
    put("startMinutes", startMinutes)
    put("endMinutes", endMinutes)
    put("enabled", enabled)
}

private fun JSONObject.toSnapshot(): FilterSnapshot = FilterSnapshot(
    version = getLong("version"),
    contactsAllowed = optBoolean("contactsAllowed", true),
    emergencyNumbersAlwaysAllowed = optBoolean("emergencyNumbersAlwaysAllowed", true),
    profiles = getJSONArray("profiles").toList { it.toProfile() },
)

private fun JSONObject.toProfile(): FilterProfile = FilterProfile(
    id = getLong("id"),
    name = getString("name"),
    enabled = getBoolean("enabled"),
    priority = getInt("priority"),
    activeWindow = optJSONObject("activeWindow")?.toTimeWindow(),
    defaultDecision = getDecision("defaultDecision"),
    rules = getJSONArray("rules").toList { it.toRule() },
)

private fun JSONObject.toRule(): FilterPolicyRule = FilterPolicyRule(
    id = getLong("id"),
    condition = PolicyCondition(
        type = PolicyMatchType.valueOf(getString("conditionType")),
        value = if (isNull("conditionValue")) null else optString("conditionValue").ifBlank { null },
    ),
    label = getString("label"),
    enabled = getBoolean("enabled"),
    decision = getDecision("decision"),
    priority = getInt("priority"),
    timeWindow = optJSONObject("timeWindow")?.toTimeWindow(),
)

private fun JSONObject.toTimeWindow(): TimeWindow = TimeWindow(
    daysOfWeek = getJSONArray("daysOfWeek").toIntSet(),
    startMinutes = getInt("startMinutes"),
    endMinutes = getInt("endMinutes"),
    enabled = getBoolean("enabled"),
)

private fun JSONObject.getDecision(key: String): CallDecision {
    return runCatching { CallDecision.valueOf(getString(key)) }
        .getOrDefault(CallDecision.BLOCK)
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    put(key, value ?: JSONObject.NULL)
}

private inline fun <T> JSONArray.toList(transform: (JSONObject) -> T): List<T> = buildList {
    for (index in 0 until length()) add(transform(getJSONObject(index)))
}

private fun JSONArray.toIntSet(): Set<Int> = buildSet {
    for (index in 0 until length()) add(getInt(index))
}
