package com.pocketdev.app

import android.app.Application
import org.eclipse.tm4e.core.registry.IThemeSource
import org.json.JSONObject

/**
 * Application class that initializes TextMate syntax highlighting
 * for sora-editor v0.23.4. Uses reflection throughout because the
 * sora-editor registry API (GrammarRegistry, ThemeRegistry, etc.)
 * changed significantly between 0.22.x and 0.23.x.
 */
class PocketDevApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            initTextMate()
        } catch (t: Throwable) {
            android.util.Log.e("PocketDev", "TextMate init failed", t)
        }
    }

    private fun initTextMate() {
        val assetsResolver = createAssetsFileResolver() ?: return

        // Register file provider
        runCatching {
            val registryClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry")
            val instance = registryClass.getMethod("getInstance").invoke(null)
            // Try addFileProvider (common API)
            runCatching {
                instance.javaClass.getMethod("addFileProvider", assetsResolver.javaClass.interfaces.firstOrNull()
                    ?: assetsResolver.javaClass)
                    .invoke(instance, assetsResolver)
            }.recoverCatching {
                // Try addFileResolver (some versions)
                instance.javaClass.getMethod("addFileResolver", assetsResolver.javaClass.interfaces.firstOrNull()
                    ?: assetsResolver.javaClass)
                    .invoke(instance, assetsResolver)
            }
        }

        // Load grammars
        runCatching {
            val languagesJson = assets.open("textmate/languages.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(languagesJson)
            val languagesArray = jsonObject.getJSONArray("languages")

            val grammarRegistryClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry")
            val grammarRegistry = grammarRegistryClass.getMethod("getInstance").invoke(null)

            for (i in 0 until languagesArray.length()) {
                val lang = languagesArray.getJSONObject(i)
                val scopeName = lang.getString("scopeName")
                val grammarPath = lang.getString("grammar")

                // Try loadGrammar with GrammarDefinition
                runCatching {
                    val grammarDefClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition")
                    val grammarDef = grammarDefClass.getConstructor(String::class.java, String::class.java)
                        .newInstance(scopeName, grammarPath)
                    grammarRegistryClass.getMethod("loadGrammar", grammarDefClass)
                        .invoke(grammarRegistry, grammarDef)
                }.recoverCatching {
                    // Try loadGrammar with (scopeName, path) directly
                    grammarRegistryClass.getMethod("loadGrammar", String::class.java, String::class.java)
                        .invoke(grammarRegistry, scopeName, grammarPath)
                }.recoverCatching {
                    // Try loadGrammars("textmate/languages.json") — bulk load
                    grammarRegistryClass.getMethod("loadGrammars", String::class.java)
                        .invoke(grammarRegistry, "textmate/languages.json")
                    return@runCatching // Only call bulk load once
                }
            }
        }.recoverCatching {
            // Fallback: bulk load
            val grammarRegistryClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry")
            val grammarRegistry = grammarRegistryClass.getMethod("getInstance").invoke(null)
            runCatching {
                grammarRegistryClass.getMethod("loadGrammars", String::class.java)
                    .invoke(grammarRegistry, "textmate/languages.json")
            }
        }

        // Load theme
        runCatching {
            val themeRegistryClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry")
            val themeRegistry = themeRegistryClass.getMethod("getInstance").invoke(null)

            val themePath = "textmate/darcula.json"
            val themeInput = assets.open(themePath)
            val themeSource = IThemeSource.fromInputStream(themeInput, themePath, null)

            // Try different ThemeModel constructors
            val model = runCatching {
                val modelClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel")
                modelClass.getConstructor(IThemeSource::class.java, String::class.java)
                    .newInstance(themeSource, "darcula")
            }.recoverCatching {
                val modelClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel")
                modelClass.getConstructor(IThemeSource::class.java)
                    .newInstance(themeSource)
            }.getOrThrow()

            // Try setDark
            runCatching {
                model.javaClass.getMethod("setDark", Boolean::class.javaPrimitiveType)
                    .invoke(model, true)
            }

            themeRegistryClass.getMethod("loadTheme", model.javaClass.interfaces.firstOrNull()
                ?: model.javaClass)
                .invoke(themeRegistry, model)

            runCatching {
                themeRegistryClass.getMethod("setTheme", String::class.java)
                    .invoke(themeRegistry, "darcula")
            }
        }
    }

    private fun createAssetsFileResolver(): Any? {
        return try {
            val resolverClass = Class.forName("io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver")
            resolverClass.getConstructor(android.content.res.AssetManager::class.java)
                .newInstance(assets)
        } catch (_: Throwable) {
            android.util.Log.w("PocketDev", "AssetsFileResolver not found, skipping TextMate init")
            null
        }
    }
}
