package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.AppRef
import com.example.model.BackgroundStyle
import com.example.model.ClockStyle
import com.example.model.DockStyle
import com.example.model.GridDensity
import com.example.model.HomeItem
import com.example.model.LauncherSettings
import com.example.model.TransitionSpeed
import com.example.stability.StabilityMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.monoGlassDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mono_glass_prefs"
)

class PreferenceManager(context: Context) {
    private val dataStore = context.applicationContext.monoGlassDataStore

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val ICON_SIZE_DP = floatPreferencesKey("icon_size_dp")
        val GRID_DENSITY = stringPreferencesKey("grid_density")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
        val TRANSITION_SPEED = stringPreferencesKey("transition_speed")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val DOCK_STYLE = stringPreferencesKey("dock_style")
        val SORT_ALPHABETICALLY = booleanPreferencesKey("sort_alphabetically")
        val HOME_ITEMS_JSON = stringPreferencesKey("home_items_json")
        val DOCK_ITEMS_JSON = stringPreferencesKey("dock_items_json")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val RECENT_PACKAGES_JSON = stringPreferencesKey("recent_packages_json")
        val LAYOUT_INITIALIZED = booleanPreferencesKey("layout_initialized")
    }

    private val safeData: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            StabilityMonitor.recordHandledError("DataStoreRead", exception)
            emit(emptyPreferences())
        } else {
            StabilityMonitor.recordHandledError("DataStoreFatal", exception)
            emit(emptyPreferences())
        }
    }

    val settingsFlow: Flow<LauncherSettings> = safeData.map { prefs ->
        LauncherSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            iconSizeDp = (prefs[Keys.ICON_SIZE_DP] ?: 56f).coerceIn(42f, 76f),
            gridDensity = enumValueOrDefault(prefs[Keys.GRID_DENSITY], GridDensity.BALANCED_4X5),
            glassOpacity = (prefs[Keys.GLASS_OPACITY] ?: 0.12f).coerceIn(0.04f, 0.28f),
            enableBlur = prefs[Keys.ENABLE_BLUR] ?: true,
            transitionSpeed = enumValueOrDefault(prefs[Keys.TRANSITION_SPEED], TransitionSpeed.SMOOTH),
            clockStyle = enumValueOrDefault(prefs[Keys.CLOCK_STYLE], ClockStyle.EDITORIAL),
            backgroundStyle = enumValueOrDefault(prefs[Keys.BACKGROUND_STYLE], BackgroundStyle.PURE_BLACK),
            dockStyle = enumValueOrDefault(prefs[Keys.DOCK_STYLE], DockStyle.FLOATING_PILL),
            sortAlphabetically = prefs[Keys.SORT_ALPHABETICALLY] ?: true
        )
    }

    val homeItemsFlow: Flow<List<HomeItem>?> = safeData.map { prefs ->
        val initialized = prefs[Keys.LAYOUT_INITIALIZED] ?: false
        if (!initialized) null else decodeHomeItems(prefs[Keys.HOME_ITEMS_JSON])
    }

    val dockItemsFlow: Flow<List<AppRef>?> = safeData.map { prefs ->
        val initialized = prefs[Keys.LAYOUT_INITIALIZED] ?: false
        if (!initialized) null else decodeAppRefs(prefs[Keys.DOCK_ITEMS_JSON])
    }

    val hiddenPackagesFlow: Flow<Set<String>> = safeData.map { prefs ->
        prefs[Keys.HIDDEN_PACKAGES] ?: emptySet()
    }

    val recentKeysFlow: Flow<List<String>> = safeData.map { prefs ->
        decodeStringList(prefs[Keys.RECENT_PACKAGES_JSON])
    }

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        runCatching {
            dataStore.edit { prefs ->
                val current = LauncherSettings(
                    language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
                    iconSizeDp = (prefs[Keys.ICON_SIZE_DP] ?: 56f).coerceIn(42f, 76f),
                    gridDensity = enumValueOrDefault(prefs[Keys.GRID_DENSITY], GridDensity.BALANCED_4X5),
                    glassOpacity = (prefs[Keys.GLASS_OPACITY] ?: 0.12f).coerceIn(0.04f, 0.28f),
                    enableBlur = prefs[Keys.ENABLE_BLUR] ?: true,
                    transitionSpeed = enumValueOrDefault(prefs[Keys.TRANSITION_SPEED], TransitionSpeed.SMOOTH),
                    clockStyle = enumValueOrDefault(prefs[Keys.CLOCK_STYLE], ClockStyle.EDITORIAL),
                    backgroundStyle = enumValueOrDefault(prefs[Keys.BACKGROUND_STYLE], BackgroundStyle.PURE_BLACK),
                    dockStyle = enumValueOrDefault(prefs[Keys.DOCK_STYLE], DockStyle.FLOATING_PILL),
                    sortAlphabetically = prefs[Keys.SORT_ALPHABETICALLY] ?: true
                )
                val next = transform(current)
                prefs[Keys.LANGUAGE] = next.language.code
                prefs[Keys.ICON_SIZE_DP] = next.iconSizeDp
                prefs[Keys.GRID_DENSITY] = next.gridDensity.name
                prefs[Keys.GLASS_OPACITY] = next.glassOpacity
                prefs[Keys.ENABLE_BLUR] = next.enableBlur
                prefs[Keys.TRANSITION_SPEED] = next.transitionSpeed.name
                prefs[Keys.CLOCK_STYLE] = next.clockStyle.name
                prefs[Keys.BACKGROUND_STYLE] = next.backgroundStyle.name
                prefs[Keys.DOCK_STYLE] = next.dockStyle.name
                prefs[Keys.SORT_ALPHABETICALLY] = next.sortAlphabetically
            }
        }.onFailure { StabilityMonitor.recordHandledError("UpdateSettings", it) }
    }

    suspend fun saveLayout(homeItems: List<HomeItem>, dockItems: List<AppRef>) {
        runCatching {
            dataStore.edit { prefs ->
                prefs[Keys.HOME_ITEMS_JSON] = encodeHomeItems(homeItems)
                prefs[Keys.DOCK_ITEMS_JSON] = encodeAppRefs(dockItems)
                prefs[Keys.LAYOUT_INITIALIZED] = true
            }
        }.onFailure { StabilityMonitor.recordHandledError("SaveLayout", it) }
    }

    suspend fun saveHomeItems(homeItems: List<HomeItem>) {
        runCatching {
            dataStore.edit { prefs ->
                prefs[Keys.HOME_ITEMS_JSON] = encodeHomeItems(homeItems)
                prefs[Keys.LAYOUT_INITIALIZED] = true
            }
        }.onFailure { StabilityMonitor.recordHandledError("SaveHomeItems", it) }
    }

    suspend fun saveDockItems(dockItems: List<AppRef>) {
        runCatching {
            dataStore.edit { prefs ->
                prefs[Keys.DOCK_ITEMS_JSON] = encodeAppRefs(dockItems)
                prefs[Keys.LAYOUT_INITIALIZED] = true
            }
        }.onFailure { StabilityMonitor.recordHandledError("SaveDockItems", it) }
    }

    suspend fun resetLayoutFlag() {
        runCatching {
            dataStore.edit { prefs ->
                prefs[Keys.LAYOUT_INITIALIZED] = false
                prefs.remove(Keys.HOME_ITEMS_JSON)
                prefs.remove(Keys.DOCK_ITEMS_JSON)
            }
        }.onFailure { StabilityMonitor.recordHandledError("ResetLayout", it) }
    }

    suspend fun setPackageHidden(packageName: String, hidden: Boolean) {
        runCatching {
            dataStore.edit { prefs ->
                val current = prefs[Keys.HIDDEN_PACKAGES] ?: emptySet()
                prefs[Keys.HIDDEN_PACKAGES] = if (hidden) current + packageName else current - packageName
            }
        }.onFailure { StabilityMonitor.recordHandledError("SetHidden", it) }
    }

    suspend fun recordRecentLaunch(componentKey: String) {
        runCatching {
            dataStore.edit { prefs ->
                val current = decodeStringList(prefs[Keys.RECENT_PACKAGES_JSON])
                val updated = (listOf(componentKey) + current.filterNot { it == componentKey }).take(12)
                prefs[Keys.RECENT_PACKAGES_JSON] = encodeStringList(updated)
            }
        }.onFailure { StabilityMonitor.recordHandledError("RecordRecent", it) }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T {
        if (raw == null) return fallback
        return try {
            enumValueOf<T>(raw)
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }

    private fun encodeHomeItems(items: List<HomeItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            when (item) {
                is HomeItem.AppShortcut -> {
                    obj.put("type", "app")
                    obj.put("pkg", item.app.packageName)
                    obj.put("act", item.app.activityName)
                    obj.put("label", item.app.label)
                }
                is HomeItem.Folder -> {
                    obj.put("type", "folder")
                    obj.put("name", item.name)
                    obj.put("apps", JSONArray(encodeAppRefs(item.apps)))
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeHomeItems(raw: String?): List<HomeItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    when (obj.optString("type")) {
                        "app" -> {
                            val pkg = obj.optString("pkg")
                            val act = obj.optString("act")
                            val label = obj.optString("label")
                            if (pkg.isNotBlank() && act.isNotBlank()) {
                                add(HomeItem.AppShortcut(id, AppRef(pkg, act, label)))
                            }
                        }
                        "folder" -> {
                            val name = obj.optString("name", "Folder")
                            val appsArr = obj.optJSONArray("apps")?.toString()
                            add(HomeItem.Folder(id, name, decodeAppRefs(appsArr)))
                        }
                    }
                }
            }
        }.onFailure { StabilityMonitor.recordHandledError("DecodeHome", it) }
            .getOrDefault(emptyList())
    }

    private fun encodeAppRefs(apps: List<AppRef>): String {
        val array = JSONArray()
        apps.forEach { app ->
            array.put(
                JSONObject().apply {
                    put("pkg", app.packageName)
                    put("act", app.activityName)
                    put("label", app.label)
                }
            )
        }
        return array.toString()
    }

    private fun decodeAppRefs(raw: String?): List<AppRef> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val pkg = obj.optString("pkg")
                    val act = obj.optString("act")
                    val label = obj.optString("label")
                    if (pkg.isNotBlank() && act.isNotBlank()) {
                        add(AppRef(pkg, act, label))
                    }
                }
            }
        }.onFailure { StabilityMonitor.recordHandledError("DecodeRefs", it) }
            .getOrDefault(emptyList())
    }

    private fun encodeStringList(list: List<String>): String = JSONArray(list).toString()

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }
}
