@file:Suppress("SpellCheckingInspection")

package pl.lambada.songsync.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.lambada.songsync.R
import pl.lambada.songsync.data.MainViewModel
import pl.lambada.songsync.data.dto.Release
import pl.lambada.songsync.data.ext.getVersion
import pl.lambada.songsync.ui.components.AboutCard
import pl.lambada.songsync.ui.components.CommonTextField
import java.io.FileNotFoundException

/**
 * Composable function for AboutScreen component.
 */
@Composable
fun AboutScreen(viewModel: MainViewModel) {
    val uriHandler = LocalUriHandler.current
    val version = LocalContext.current.getVersion()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sharedPreferences = context.getSharedPreferences(
        "pl.lambada.songsync_preferences",
        Context.MODE_PRIVATE
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        item {
            AboutCard(
                label = stringResource(R.string.way_to_get_token),
                columnModifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            ) {
                var selected by remember { mutableIntStateOf(viewModel.tokenType) }
                val hasDefaultKeys = viewModel.isBuiltWithKeys
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(hasDefaultKeys) {
                        selected = 0
                        viewModel.tokenType = 0
                        sharedPreferences.edit().putInt("token_type", 0).apply()
                    }
                ) {
                    RadioButton(
                        selected = selected == 0,
                        onClick = {
                            selected = 0
                            viewModel.tokenType = 0
                            sharedPreferences.edit().putInt("token_type", 0).apply()
                        },
                        enabled = hasDefaultKeys
                    )
                    Text(stringResource(R.string.default_keys))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        selected = 1
                        viewModel.tokenType = 1
                        sharedPreferences.edit().putInt("token_type", 1).apply()
                    }
                ) {
                    RadioButton(
                        selected = selected == 1,
                        onClick = {
                            selected = 1
                            viewModel.tokenType = 1
                            sharedPreferences.edit().putInt("token_type", 1).apply()
                        },
                    )
                    Text(stringResource(R.string.web_player_keys))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        selected = 2
                        viewModel.tokenType = 2
                        sharedPreferences.edit().putInt("token_type", 2).apply()
                    }
                ) {
                    RadioButton(
                        selected = selected == 2,
                        onClick = {
                            selected = 2
                            viewModel.tokenType = 2
                            sharedPreferences.edit().putInt("token_type", 2).apply()
                        },
                    )
                    Text(stringResource(R.string.custom_keys))
                }

                when (selected) {
                    0, 1 -> {
                        LaunchedEffect(selected) {
                            viewModel.tokenTime = 0
                            launch(Dispatchers.IO) {
                                viewModel.refreshToken()
                            }
                        }
                    }
                    2 -> {
                        val customID = sharedPreferences.getString("custom_id", null)
                        var customDisplayID by rememberSaveable { mutableStateOf(customID ?: "") }
                        val customSecret = sharedPreferences.getString("custom_secret", null)
                        var customDisplaySecret by rememberSaveable { mutableStateOf(customSecret ?: "") }

                        CommonTextField(
                            label = stringResource(R.string.spotify_client_id),
                            value = customDisplayID,
                            onValueChange = { customDisplayID = it.toString() },
                            imeAction = ImeAction.Next,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        CommonTextField(
                            label = stringResource(R.string.spotify_client_secret),
                            value = customDisplaySecret,
                            onValueChange = { customDisplaySecret = it.toString() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                modifier = Modifier.padding(top = 8.dp),
                                onClick = {
                                    if (customDisplayID == "" || customDisplaySecret == "") {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.cant_be_empty),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    sharedPreferences.edit()
                                        .putString("custom_id", customDisplayID)
                                        .putString("custom_secret", customDisplaySecret)
                                        .apply()
                                    viewModel.customID = customDisplayID
                                    viewModel.customSecret = customDisplaySecret
                                    viewModel.tokenTime = 0

                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            viewModel.refreshToken()
                                        } catch (e: FileNotFoundException) {
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.invalid_credentials),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            viewModel.customID = ""
                                            viewModel.customSecret = ""
                                            viewModel.tokenType = if (hasDefaultKeys) 0 else 1
                                            sharedPreferences.edit().remove("token_type").apply()
                                            viewModel.tokenTime = 0
                                            launch(Dispatchers.IO) {
                                                viewModel.refreshToken()
                                            }
                                            return@launch
                                        }
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.credentials_saved),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }

        item {
            if (isSystemInDarkTheme()) {
                AboutCard(label = stringResource(R.string.theme)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.pure_black_theme))
                        Spacer(modifier = Modifier.weight(1f))
                        val pureBlack = viewModel.pureBlack
                        var selected by remember { mutableStateOf(pureBlack) }
                        Switch(
                            checked = selected,
                            onCheckedChange = {
                                viewModel.pureBlack = it
                                selected = it
                                sharedPreferences.edit().putBoolean("pure_black", it).apply()
                            }
                        )
                    }
                    Text(
                        text = stringResource(R.string.restart_the_app),
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }
            }
        }

        item {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                var picker by remember { mutableStateOf(false) }
                val sdCardPath = viewModel.sdCardPath
                var sdPath by rememberSaveable { mutableStateOf(sdCardPath) }
                AboutCard(label = stringResource(R.string.sd_card)) {
                    Text(stringResource(R.string.set_sd_path))
                    if(sdPath == "" || sdPath == null) {
                        Text(
                            text = stringResource(R.string.no_sd_card_path_set),
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sd_card_path_set_successfully),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = {
                                sdPath = ""
                                viewModel.sdCardPath = ""
                                sharedPreferences.edit().remove("sd_card_path").apply()
                            }
                        ) {
                            Text(stringResource(R.string.clear_sd_card_path))
                        }
                    }
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            modifier = Modifier.padding(top = 8.dp),
                            onClick = {
                                picker = true
                            }
                        ) {
                            Text(stringResource(R.string.set_sd_card_path))
                        }
                    }

                    if (picker) {
                        val sdCardPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
                            if (it == null) {
                                picker = false
                                return@rememberLauncherForActivityResult
                            }
                            sdPath = it.toString()
                            viewModel.sdCardPath = it.toString()
                            sharedPreferences.edit().putString("sd_card_path", it.toString()).apply()
                            picker = false
                        }
                        LaunchedEffect(Unit) {
                            sdCardPicker.launch(Uri.parse(Environment.getExternalStorageDirectory().absolutePath))
                        }
                    }
                }
            }
        }

        item {
            var update by rememberSaveable { mutableStateOf(false) }
            AboutCard(stringResource(R.string.about_songsync)) {
                Text(stringResource(R.string.what_is_songsync))
                Text(stringResource(R.string.extra_what_is_songsync))
                Text("")
                Text(stringResource(R.string.app_version, version))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = { update = true }
                    ) {
                        Text(stringResource(R.string.check_for_updates))
                    }
                }
            }

            if (update) {
                CheckForUpdates(
                    onDismiss = { update = false },
                    onDownload = { uriHandler.openUri(it) },
                    context = LocalContext.current,
                    viewModel = viewModel,
                    version = version
                )
            }
        }
        item {
            AboutCard(stringResource(R.string.spotify_api)) {
                Text(stringResource(R.string.what_it_uses))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            uriHandler.openUri("https://developer.spotify.com/documentation/web-api")
                        }
                    ) {
                        Text(stringResource(R.string.spotify_for_developers))
                    }
                }
            }
        }
        item {
            AboutCard(stringResource(R.string.spotifylyrics_api)) {
                Text(stringResource(R.string.how_we_get_lyrics))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            uriHandler.openUri("https://github.com/akashrchandran/spotify-lyrics-api")
                        }
                    ) {
                        Text(stringResource(R.string.view_on_github))
                    }
                }
            }
        }
        item {
            AboutCard(stringResource(R.string.source_code)) {
                Text(stringResource(R.string.we_are_open_source))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        onClick = {
                            uriHandler.openUri("https://github.com/Lambada10/SongSync")
                        }
                    ) {
                        Text(stringResource(id = R.string.view_on_github))
                    }
                }
            }
        }
        item {
            AboutCard(stringResource(R.string.contributors)) {
                Contributor.values().forEach {
                    val additionalInfo = stringResource(id = it.contributionLevel.stringResource)
                    Text(text = "${it.devName} ($additionalInfo)")
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        if (it.github != null) {
                            Button(
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                onClick = {
                                    uriHandler.openUri(it.github)
                                }
                            ) {
                                Text(stringResource(R.string.github))
                            }
                        }
                        if (it.telegram != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                onClick = {
                                    uriHandler.openUri(it.telegram)
                                }
                            ) {
                                Text(stringResource(R.string.telegram))
                            }
                        }
                    }
                }
            }
        }
        item {
            AboutCard(stringResource(R.string.support)) {
                Text(stringResource(R.string.bugs_or_suggestions_contact_us))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        onClick = {
                            uriHandler.openUri("https://t.me/LambadaOT")
                        }
                    ) {
                        Text(stringResource(R.string.telegram_group))
                    }
                }
                Text(stringResource(R.string.create_issue))
            }
        }
    }
}

@Composable
fun CheckForUpdates(
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    context: Context,
    viewModel: MainViewModel,
    version: String
) {
    var updateState by rememberSaveable { mutableStateOf(UpdateState.CHECKING) }
    var latest by rememberSaveable { mutableStateOf(Release()) }
    var isUpdate by rememberSaveable { mutableStateOf(false) }

    when (updateState) {
        UpdateState.CHECKING -> {
            Toast.makeText(
                context,
                stringResource(R.string.checking_for_updates),
                Toast.LENGTH_SHORT
            ).show()

            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) {
                    try {
                        latest = viewModel.getLatestRelease()
                        isUpdate = viewModel.isNewerRelease(context)
                    } catch (e: Exception) {
                        updateState = UpdateState.ERROR
                        return@launch
                    }
                    updateState = if (isUpdate) {
                        UpdateState.UPDATE_AVAILABLE
                    } else {
                        UpdateState.UP_TO_DATE
                    }
                }
            }
        }
        UpdateState.UPDATE_AVAILABLE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.update_available)) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text("v$version -> ${latest.tagName}")
                        Text(stringResource(R.string.changelog, latest.changelog!!))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDownload(latest.htmlURL!!)
                        }
                    ) {
                        Text(stringResource(R.string.download))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        UpdateState.UP_TO_DATE -> {
            Toast.makeText(
                context,
                stringResource(R.string.up_to_date),
                Toast.LENGTH_SHORT
            ).show()
            onDismiss()
        }
        UpdateState.ERROR -> {
            Toast.makeText(
                context,
                stringResource(R.string.error_checking_for_updates),
                Toast.LENGTH_SHORT
            ).show()
            onDismiss()
        }
    }
}

@Suppress("SpellCheckingInspection")
enum class Contributor(val devName: String, val contributionLevel: ContributionLevel,
                       val github: String? = null, val telegram: String? = null) {
    LAMBADA10("Lambada10", ContributionLevel.LEAD_DEVELOPER,
        github = "https://github.com/Lambada10", telegram = "https://t.me/Lambada10"),
    NIFT4("Nick", ContributionLevel.DEVELOPER,
        github = "https://github.com/nift4", telegram = "https://t.me/nift4"),
    BOBBYESP("BobbyESP", ContributionLevel.CONTRIBUTOR,
        github = "https://github.com/BobbyESP"),
    AKANETAN("AkaneTan", ContributionLevel.CONTRIBUTOR,
        github = "https://github.com/AkaneTan")
}

/**
 * Defines the contribution level of a contributor.
 */
enum class ContributionLevel(val stringResource: Int) {
    CONTRIBUTOR(R.string.contributor),
    DEVELOPER(R.string.developer),
    LEAD_DEVELOPER(R.string.lead_developer)
}

/**
 * Defines the state of the update check.
 */
enum class UpdateState {
    CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR
}