package dev.benkidd.thornav

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dev.benkidd.thornav.utils.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ThorNavFixTileService : TileService(), CoroutineScope {
    private val permissionsHelper: PermissionsHelper by inject()
    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Main + job

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.tile_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        launch {
            val success = permissionsHelper.applyFix()

            Toast.makeText(
                this@ThorNavFixTileService,
                if (success) R.string.msg_tile_fix_success else R.string.msg_tile_fix_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
    }
}
