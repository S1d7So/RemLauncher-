
package com.remlauncher.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class MainActivity : Activity() {

    companion object {
        const val VERSION = "1.21.4"
        const val MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

        const val BG = 0xFF18181B.toInt()
        const val CARD = 0xFF242429.toInt()
        const val GREEN = 0xFF10B981.toInt()
        const val TEXT = 0xFFF4F4F5.toInt()
        const val MUTED = 0xFFA1A1AA.toInt()
    }

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var console: TextView
    private lateinit var progress: ProgressBar

    private var nickname = "RemPlayer"
    private var ram = 4096
    private var renderer = "KlodexGpu"
    private var profile = "1.21.4 Vanilla (Normal)"
    private var selectedTab = "Jogar"

    private val prefs by lazy {
        getSharedPreferences("rem_config", MODE_PRIVATE)
    }

    private val launcherDir by lazy {
        File(filesDir, "rem_launcher").apply {
            mkdirs()
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            console.append("> $message\n")
        }
    }

    private fun saveConfig() {
        prefs.edit()
            .putString("nickname", nickname)
            .putInt("ram", ram)
            .putString("renderer", renderer)
            .putString("profile", profile)
            .apply()
    }

    private fun loadConfig() {
        nickname = prefs.getString("nickname", "RemPlayer") ?: "RemPlayer"
        ram = prefs.getInt("ram", 4096)
        renderer = prefs.getString("renderer", "KlodexGpu") ?: "KlodexGpu"
        profile = prefs.getString(
            "profile",
            "1.21.4 Vanilla (Normal)"
        ) ?: "1.21.4 Vanilla (Normal)"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.decorView.systemUiVisibility = 0

        loadConfig()
        createDefaultControls()
        buildInterface()
    }

    private fun createDefaultControls() {
        val file = File(launcherDir, "controls.json")

        if (!file.exists()) {
            val controls = JSONObject()
            controls.put("scale", 1.0)
            controls.put("opacity", 0.75)

            val buttons = org.json.JSONArray()

            val defaults = listOf(
                Triple("W", 0.12, 0.65),
                Triple("A", 0.06, 0.76),
                Triple("S", 0.12, 0.87),
                Triple("D", 0.18, 0.76),
                Triple("SHIFT", 0.12, 0.96),
                Triple("SPACE", 0.86, 0.88),
                Triple("PRI", 0.94, 0.72),
                Triple("SEC", 0.78, 0.72),
                Triple("INV", 0.94, 0.88),
                Triple("ESC", 0.04, 0.06),
                Triple("F3", 0.12, 0.06),
                Triple("T", 0.20, 0.06)
            )

            defaults.forEach { (name, x, y) ->
                val item = JSONObject()
                item.put("name", name)
                item.put("x", x)
                item.put("y", y)
                item.put("visible", true)
                buttons.put(item)
            }

            controls.put("buttons", buttons)

            file.writeText(controls.toString(2))
        }
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(16, 12, 16, 12)
        }

        val header = TextView(this).apply {
            text = "◆  REM LAUNCHER"
            textSize = 22f
            setTextColor(GREEN)
            setPadding(8, 16, 8, 16)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        root.addView(header)

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 12)
        }

        listOf("Jogar", "Configurações", "Controles").forEach { tab ->
            val button = Button(this).apply {
                text = tab
                textSize = 11f
                isAllCaps = false
                setTextColor(if (selectedTab == tab) GREEN else TEXT)
                setBackgroundColor(CARD)
                setOnClickListener {
                    selectedTab = tab
                    buildInterface()
                }
            }

            navigation.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    48,
                    1f
                ).apply {
                    setMargins(3, 0, 3, 0)
                }
            )
        }

        root.addView(navigation)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(GREEN)
            progress = 0
        }

        root.addView(progress)

        val consoleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            setPadding(10, 8, 10, 8)
        }

        consoleCard.addView(
            TextView(this).apply {
                text = "CONSOLE / LOGS"
                textSize = 11f
                setTextColor(GREEN)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        )

        console = TextView(this).apply {
            textSize = 10f
            setTextColor(0xFFB4F0D3.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            maxLines = 5
            text = "Rem Launcher iniciado.\nMinecraft $VERSION\n"
        }

        consoleCard.addView(console)

        root.addView(
            consoleCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                110
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        )

        setContentView(root)

        when (selectedTab) {
            "Jogar" -> showPlayTab()
            "Configurações" -> showSettingsTab()
            "Controles" -> showControlsTab()
        }
    }

    private fun addCard(title: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            setPadding(14, 14, 14, 14)
        }

        card.addView(
            TextView(this).apply {
                text = title
                textSize = 16f
                setTextColor(GREEN)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
        )

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        return card
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float = 14f
    ) {
        parent.addView(
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(TEXT)
                setPadding(0, 7, 0, 7)
            }
        )
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        color: Int = GREEN,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.BLACK)
            setBackgroundColor(color)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        )
    }

    private fun showPlayTab() {
        val account = addCard("OFFLINE ACCOUNT")

        addText(account, "Nickname da conta")

        val nicknameInput = EditText(this).apply {
            setSingleLine(true)
            setText(nickname)
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            hint = "RemPlayer"
            setBackgroundColor(BG)
            setPadding(12, 8, 12, 8)
        }

        account.addView(nicknameInput)

        nicknameInput.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    nickname = s.toString()
                    saveConfig()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )

        addText(account, "Modo offline • Sem login Microsoft", 11f)

        val versionCard = addCard("VERSÃO / PERFIL")

        val profiles = listOf(
            "1.21.4 Vanilla (Normal)",
            "1.21.4 NeoForge",
            "1.21.4 OptiFine"
        )

        val profileSpinner = Spinner(this)

        profileSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            profiles
        )

        profileSpinner.setSelection(
            profiles.indexOf(profile).coerceAtLeast(0)
        )

        profileSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    profile = profiles[position]
                    saveConfig()
                    updateInstallerButton(versionCard)
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        versionCard.addView(profileSpinner)

        updateInstallerButton(versionCard)

        val gameCard = addCard("MINECRAFT JAVA EDITION")

        addText(gameCard, "Versão selecionada: $VERSION")
        addText(gameCard, "Renderizador: $renderer")
        addText(gameCard, "RAM: $ram MB")

        addButton(
            gameCard,
            "JOGAR 1.21.4"
        ) {
            if (!validNickname()) {
                return@addButton
            }

            saveConfig()
            downloadMinecraft()
        }

        addText(
            gameCard,
            "Os arquivos são armazenados no espaço privado do aplicativo.",
            11f
        )
    }

    private fun updateInstallerButton(card: LinearLayout) {
        val existing = card.findViewWithTag<View>("installer_button")
        if (existing != null) {
            card.removeView(existing)
        }

        val label = when {
            profile.contains("NeoForge") ->
                "Baixar / Instalar NeoForge 1.21.4"

            profile.contains("OptiFine") ->
                "Selecionar / Instalar OptiFine 1.21.4"

            else -> null
        }

        if (label != null) {
            val button = Button(this).apply {
                tag = "installer_button"
                text = label
                isAllCaps = false
                setTextColor(TEXT)
                setBackgroundColor(0xFF3F3F46.toInt())

                setOnClickListener {
                    if (profile.contains("OptiFine")) {
                        chooseOptiFineJar()
                    } else {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("NeoForge 1.21.4")
                            .setMessage(
                                "O instalador Java do NeoForge requer um runtime Java Android e integração de instalação. " +
                                "Essa etapa ainda não está implementada nesta versão."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }

            card.addView(button)
        }
    }

    private fun validNickname(): Boolean {
        val valid = nickname.matches(
            Regex("^[A-Za-z0-9_]{1,16}$")
        )

        if (!valid) {
            Toast.makeText(
                this,
                "Nickname inválido. Use letras, números e _ (máximo 16).",
                Toast.LENGTH_LONG
            ).show()
        }

        return valid
    }

    private fun chooseOptiFineJar() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/java-archive"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            startActivityForResult(intent, 1001)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Não foi possível abrir o seletor de arquivos.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Deprecated("Usado para compatibilidade com o seletor de arquivos.")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            prefs.edit()
                .putString("optifine_uri", uri.toString())
                .apply()

            log("Arquivo OptiFine selecionado: $uri")

            Toast.makeText(
                this,
                "Arquivo selecionado. A instalação Java ainda precisa ser integrada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showSettingsTab() {
        val memoryCard = addCard("ALOCAÇÃO DE MEMÓRIA RAM")

        val ramLabel = TextView(this).apply {
            text = "$ram MB"
            textSize = 18f
            setTextColor(GREEN)
        }

        memoryCard.addView(ramLabel)

        val slider = SeekBar(this).apply {
            max = 7168
            progress = ram.coerceIn(1024, 8192) - 1024
            progressTintList =
                android.content.res.ColorStateList.valueOf(GREEN)
            thumbTintList =
                android.content.res.ColorStateList.valueOf(GREEN)
        }

        memoryCard.addView(slider)

        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    value: Int,
                    fromUser: Boolean
                ) {
                    ram = 1024 + (value / 256) * 256
                    ramLabel.text = "$ram MB"
                    saveConfig()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        addText(memoryCard, "-Xms512M -Xmx${ram}M", 11f)

        val renderCard = addCard("RENDERIZADOR GRÁFICO")

        val renderers = listOf(
            "KlodexGpu",
            "GL4ES",
            "ANGLE",
            "Zink"
        )

        val spinner = Spinner(this)

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            renderers
        )

        spinner.setSelection(
            renderers.indexOf(renderer).coerceAtLeast(0)
        )

        spinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    renderer = renderers[position]
                    saveConfig()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        renderCard.addView(spinner)

        addText(
            renderCard,
            "KlodexGpu é uma opção de configuração. O motor gráfico próprio ainda precisa ser integrado.",
            11f
        )

        val optimization = addCard("KLODEXGPU OPTIMIZATION")

        addText(
            optimization,
            "Configuração planejada:\n" +
                "• Limpeza de logs antigos\n" +
                "• G1GC\n" +
                "• ParallelRefProcEnabled\n" +
                "• MaxGCPauseMillis=50\n" +
                "• LIBGL_ES=3\n" +
                "• KLODEX_GPU_OPT=1",
            12f
        )
    }

    private fun showControlsTab() {
        val card = addCard("CONTROLES MOBILE")

        addText(
            card,
            "Editor inicial dos controles virtuais."
        )

        addText(
            card,
            "Esquerda: W, A, S, D, SHIFT\n" +
                "Direita: SPACE, PRI, SEC, INV\n" +
                "Topo: ESC, F3, T"
        )

        val controlsFile = File(launcherDir, "controls.json")

        addButton(card, "ABRIR / VISUALIZAR CONTROLES") {
            val text = controlsFile.readText()

            AlertDialog.Builder(this)
                .setTitle("controls.json")
                .setMessage(text)
                .setPositiveButton("Fechar", null)
                .show()
        }

        addButton(card, "RESTAURAR CONTROLES PADRÃO", 0xFF3F3F46.toInt()) {
            controlsFile.delete()
            createDefaultControls()

            Toast.makeText(
                this,
                "Controles restaurados.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addText(
            card,
            "A edição por arraste e os ajustes individuais de posição ainda precisam de uma camada de edição dedicada.",
            11f
        )
    }

    private fun downloadMinecraft() {
        val buttonMessage = "Preparando Minecraft 1.21.4"

        log(buttonMessage)

        progress.progress = 0

        executor.execute {
            try {
                val versionsDir = File(
                    launcherDir,
                    "versions/$VERSION"
                ).apply { mkdirs() }

                val librariesDir = File(
                    launcherDir,
                    "libraries"
                ).apply { mkdirs() }

                log("Consultando manifesto da Mojang...")

                val manifest = JSONObject(
                    httpGet(MANIFEST_URL).toString(Charsets.UTF_8)
                )

                val versions = manifest.getJSONArray("versions")

                var versionUrl: String? = null

                for (i in 0 until versions.length()) {
                    val item = versions.getJSONObject(i)

                    if (item.optString("id") == VERSION) {
                        versionUrl = item.optString("url")
                        break
                    }
                }

                val jsonUrl = versionUrl
                    ?: throw Exception("Versão $VERSION não encontrada.")

                val jsonFile = File(
                    versionsDir,
                    "$VERSION.json"
                )

                if (!jsonFile.exists()) {
                    log("Baixando JSON da versão...")
                    jsonFile.writeBytes(httpGet(jsonUrl))
                }

                val versionJson = JSONObject(
                    jsonFile.readText()
                )

                runOnUiThread {
                    progress.progress = 20
                }

                val client = versionJson
                    .getJSONObject("downloads")
                    .getJSONObject("client")

                val jarFile = File(
                    versionsDir,
                    "$VERSION.jar"
                )

                downloadVerified(
                    client.getString("url"),
                    jarFile,
                    client.optString("sha1")
                )

                log("Minecraft JAR concluído.")

                runOnUiThread {
                    progress.progress = 45
                }

                val libraries = versionJson.getJSONArray("libraries")

                val total = libraries.length().coerceAtLeast(1)

                for (i in 0 until libraries.length()) {
                    val library = libraries.getJSONObject(i)

                    if (!library.has("downloads")) continue

                    val downloads = library.getJSONObject("downloads")

                    if (!downloads.has("artifact")) continue

                    val artifact = downloads.getJSONObject("artifact")

                    val relativePath = artifact.optString("path")
                    val url = artifact.optString("url")

                    if (relativePath.isBlank() || url.isBlank()) {
                        continue
                    }

                    val destination = File(
                        librariesDir,
                        relativePath
                    )

                    downloadVerified(
                        url,
                        destination,
                        artifact.optString("sha1")
                    )

                    val percent = 45 + ((i + 1) * 45 / total)

                    runOnUiThread {
                        progress.progress = percent
                    }
                }

                log("Bibliotecas verificadas e baixadas.")
                log("Download base concluído.")

                runOnUiThread {
                    progress.progress = 100

                    val intent = Intent(
                        this,
                        GameActivity::class.java
                    )

                    intent.putExtra("profile", profile)
                    intent.putExtra("nickname", nickname)
                    intent.putExtra("ram", ram)
                    intent.putExtra("renderer", renderer)

                    startActivity(intent)
                }

            } catch (error: Exception) {
                log("ERRO: ${error.message}")

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Falha no download: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun httpGet(url: String): ByteArray {
        val connection = URL(url).openConnection()
            as HttpURLConnection

        connection.connectTimeout = 20000
        connection.readTimeout = 60000
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "User-Agent",
            "RemLauncher/0.1.0 Android"
        )

        try {
            val status = connection.responseCode

            if (status !in 200..299) {
                throw Exception("HTTP $status: $url")
            }

            return connection.inputStream.use {
                it.readBytes()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadVerified(
        url: String,
        destination: File,
        expectedSha1: String
    ) {
        destination.parentFile?.mkdirs()

        if (destination.exists() && expectedSha1.isNotBlank()) {
            val current = sha1(destination)

            if (current.equals(expectedSha1, true)) {
                log("Cache: ${destination.name}")
                return
            }
        } else if (destination.exists()) {
            log("Cache: ${destination.name}")
            return
        }

        log("Baixando ${destination.name}...")

        val temporary = File(
            destination.parentFile,
            destination.name + ".part"
        )

        temporary.writeBytes(httpGet(url))

        if (expectedSha1.isNotBlank()) {
            val actual = sha1(temporary)

            if (!actual.equals(expectedSha1, true)) {
                temporary.delete()
                throw Exception(
                    "SHA-1 inválido: ${destination.name}"
                )
            }
        }

        if (destination.exists()) {
            destination.delete()
        }

        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
    }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")

        file.inputStream().use { input ->
            val buffer = ByteArray(8192)

            while (true) {
                val count = input.read(buffer)

                if (count < 0) break

                digest.update(buffer, 0, count)
            }
        }

        return digest.digest().joinToString("") {
            "%02x".format(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
