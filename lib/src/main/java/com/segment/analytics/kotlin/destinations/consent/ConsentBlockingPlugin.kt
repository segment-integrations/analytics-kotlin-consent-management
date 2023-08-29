package com.segment.analytics.kotlin.destinations.consent

import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.BaseEvent
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.platform.Plugin
import kotlinx.serialization.json.JsonObject
import sovran.kotlin.SynchronousStore


internal const val CONSENT_SETTINGS = "consent"
internal const val CATEGORY_PREFERENCE = "categoryPreference"

class ConsentBlockingPlugin(
    var destinationKey: String,
    var store: SynchronousStore,
    var allowSegmentPreferenceEvent: Boolean = true
) : Plugin {
    override lateinit var analytics: Analytics
    override val type: Plugin.Type = Plugin.Type.Enrichment
    private val TAG = "ConsentBlockingPlugin"

    override fun execute(event: BaseEvent): BaseEvent? {
        val currentState = store.currentState(ConsentState::class)

        val requiredConsentCategories = currentState?.destinationCategoryMap?.get(destinationKey)

        if (requiredConsentCategories != null && requiredConsentCategories.isNotEmpty()) {

            val consentJsonArray = getConsentCategoriesFromEvent(event)

            // Look for a missing consent category
            requiredConsentCategories.forEach {
                if (!consentJsonArray.contains(it)) {


                    if (allowSegmentPreferenceEvent && event is TrackEvent && event.event == ConsentManagementPlugin.EVENT_SEGMENT_CONSENT_PREFERENCE) {
                        // IF event is the SEGMENT CONSENT PREFERENCE event let it through
                        return event
                    } else {
                        return null
                    }
                }
            }
        }
        return event
    }

    private fun getConsentCategoriesFromEvent(event: BaseEvent): MutableList<String> {
        val consentJsonArray = mutableListOf<String>()

        val consentSettingsJson = event.context[CONSENT_SETTINGS]
        if (consentSettingsJson != null) {
            val consentJsonObject = (consentSettingsJson as JsonObject)
            val categoryPreferenceJson = consentJsonObject[CATEGORY_PREFERENCE]
            if (categoryPreferenceJson != null) {
                val categoryPreferenceJsonObject = categoryPreferenceJson as JsonObject
                categoryPreferenceJsonObject.forEach { category, consentGiven ->
                    if (consentGiven.toString() == "true") {
                        // Add this category to the list of necessary categories
                        consentJsonArray.add(category)
                    }
                }
            }
        }
        return consentJsonArray
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
    }

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
    }
}