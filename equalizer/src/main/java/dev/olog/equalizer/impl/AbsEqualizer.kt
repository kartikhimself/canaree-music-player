package dev.olog.equalizer.impl

import dev.olog.core.entity.EqualizerPreset
import dev.olog.core.gateway.EqualizerGateway
import dev.olog.core.prefs.EqualizerPreferencesGateway
import dev.olog.equalizer.IEqualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

abstract class AbsEqualizer(
    protected val gateway: EqualizerGateway,
    protected val prefs: EqualizerPreferencesGateway
) : IEqualizer {

    override fun getPresets(): List<EqualizerPreset> = gateway.getPresets()

    override fun observeCurrentPreset(): Flow<EqualizerPreset> {
        return gateway.observeCurrentPreset()
    }

    override fun getCurrentPreset(): EqualizerPreset {
        return gateway.getCurrentPreset()
    }

    override suspend fun updateCurrentPresetIfCustom() = withContext(Dispatchers.IO) {
        var preset = gateway.getCurrentPreset()
        if (preset.isCustom) {
            preset = preset.withBands(
                bands = getAllBandsCurrentLevel()
            )
            gateway.updatePreset(preset)
        }
    }

}