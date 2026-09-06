/*
 * Copyright (C) 2026 JavaNativeLink
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.polyglotrapids

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Obstacle(var x: Float, var y: Float, val type: Int, var hit: Boolean = false)
data class CollectableLetter(var x: Float, var y: Float, val character: String, var collected: Boolean = false)

class PolyglotRapidsGame(private val context: Context) {
    var status by mutableStateOf("menu") // "menu", "playing", "gameover"
    var health by mutableStateOf(100)
    var score by mutableStateOf(0)
    var raftX by mutableStateOf(0.5f)
    var invulnerableTimer by mutableStateOf(0f)
    var wordLockTimer by mutableStateOf(0f)
    var activeWord by mutableStateOf("")
    var pendingWord by mutableStateOf("")
    var currentLanguage by mutableStateOf("en")
    var wordsFound by mutableStateOf(0)

    val obstacles = mutableStateListOf<Obstacle>()
    val letters = mutableStateListOf<CollectableLetter>()
    val foundWordsList = mutableStateListOf<String>()

    var activeLanguages = listOf<String>()
    val dictionaries = mutableMapOf<String, Set<String>>()
    val dictionaryLists = mutableMapOf<String, List<String>>()

    private var speed = 0.5f
    private var spawnTimer = 0f
    private val rng = java.util.Random()

    fun reset() {
        activeWord = ""
        pendingWord = ""
        health = 100
        score = 0
        wordsFound = 0
        wordLockTimer = 0f
        currentLanguage = "en"
        foundWordsList.clear()
        letters.clear()
        obstacles.clear()
        spawnTimer = 0f
        status = "playing"
        raftX = 0.5f
        speed = 0.5f
        invulnerableTimer = 0f
    }

    fun startGame(langs: List<String>) {
        setLanguages(langs)
        reset()
        status = "playing"
    }

    private fun setLanguages(langs: List<String>) {
        activeLanguages = langs
        loadDictionaries()
    }

    private fun loadDictionaries() {
        dictionaries.clear()
        dictionaryLists.clear()
        val am = context.assets
        for (lang in activeLanguages) {
            val words = mutableSetOf<String>()
            val isCJK = lang in listOf("zh", "ja", "ko")
            try {
                am.open("dicts/$lang.txt").bufferedReader().useLines { lines ->
                    for (word in lines) {
                        val w = word.trim()
                        if (w.isEmpty()) continue
                        val len = splitIntoGraphemes(w).size
                        if (isCJK || len > 1) {
                            words.add(w)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dictionaries[lang] = words
            dictionaryLists[lang] = words.toList()
        }
    }

    private fun splitIntoGraphemes(word: String): List<String> {
        val result = mutableListOf<String>()
        val iterator = java.text.BreakIterator.getCharacterInstance()
        iterator.setText(word)
        var start = iterator.first()
        var end = iterator.next()
        while (end != java.text.BreakIterator.DONE) {
            result.add(word.substring(start, end))
            start = end
            end = iterator.next()
        }
        return result
    }

    fun moveRaft(delta: Float) {
        if (status != "playing") return
        raftX += delta
        raftX = raftX.coerceIn(0.1f, 0.9f)
    }

    fun update(dt: Float) {
        if (status != "playing") return

        if (invulnerableTimer > 0) invulnerableTimer -= dt

        if (wordLockTimer > 0) {
            wordLockTimer -= dt
            if (wordLockTimer <= 0) {
                scorePendingWord()
            }
        }

        val scrollSpeed = minOf(0.6f, speed + (wordsFound * 0.005f))
        val effectiveDensitySpeed = speed + (wordsFound * 0.01f)
        val spawnInterval = maxOf(0.25f, 0.5f / effectiveDensitySpeed)

        spawnTimer -= dt
        if (spawnTimer <= 0) {
            spawnTimer = spawnInterval
            spawnRandomEntity()
        }

        // Move obstacles
        obstacles.forEach { obs ->
            obs.y += scrollSpeed * dt
            if (!obs.hit && obs.y in 0.85f..0.95f) {
                if (kotlin.math.abs(obs.x - raftX) < 0.1f) {
                    handleObstacleCollision(obs)
                }
            }
        }

        // Move letters
        letters.forEach { letter ->
            letter.y += scrollSpeed * dt
            if (!letter.collected && letter.y in 0.85f..0.95f) {
                if (kotlin.math.abs(letter.x - raftX) < 0.1f) {
                    handleLetterCollision(letter)
                }
            }
        }

        obstacles.removeAll { it.y > 1.2f || it.hit }
        letters.removeAll { it.y > 1.2f || it.collected }

        if (health <= 0) status = "gameover"
    }

    private fun spawnRandomEntity() {
        val x = rng.nextFloat() * 0.8f + 0.1f
        val type = rng.nextInt(100)

        if (type < 30) {
            obstacles.add(Obstacle(x, -0.1f, rng.nextInt(4)))
        } else {
            letters.add(CollectableLetter(x, -0.1f, getRandomLetter()))
        }
    }

    private fun getRandomLetter(): String {
        if (activeLanguages.isEmpty()) return "A"
        val lang = activeLanguages[rng.nextInt(activeLanguages.size)]
        val dictList = dictionaryLists[lang] ?: return "A"
        if (dictList.isEmpty()) return "A"

        val word = dictList[rng.nextInt(dictList.size)]
        if (word.isEmpty()) return "A"

        val graphemes = splitIntoGraphemes(word)
        if (graphemes.isEmpty()) return "A"

        val searchWord = pendingWord + activeWord
        if (searchWord.isNotEmpty()) {
            if (rng.nextBoolean()) {
                val possibleLetters = mutableListOf<String>()
                for (w in dictList) {
                    if (w.startsWith(searchWord) && w.length > searchWord.length) {
                        val wGraphemes = splitIntoGraphemes(w)
                        val aGraphemes = splitIntoGraphemes(searchWord)
                        if (aGraphemes.size < wGraphemes.size) {
                            possibleLetters.add(wGraphemes[aGraphemes.size])
                        }
                    }
                }
                if (possibleLetters.isNotEmpty()) {
                    return possibleLetters[rng.nextInt(possibleLetters.size)]
                }
            }
        }
        return graphemes[rng.nextInt(graphemes.size)]
    }

    private fun handleObstacleCollision(obs: Obstacle) {
        obs.hit = true
        if (obs.type == 3) {
            invulnerableTimer = 1.0f
            score += 5
        } else {
            if (invulnerableTimer <= 0) {
                val damage = if (obs.type == 0) 5 else if (obs.type == 1) 3 else 1
                health -= damage
                if (wordLockTimer > 0 && pendingWord.isNotEmpty()) {
                    scorePendingWord()
                }
                activeWord = ""
            }
        }
    }

    private fun scorePendingWord() {
        if (pendingWord.isEmpty()) return
        val len = splitIntoGraphemes(pendingWord).size
        health = minOf(100, health + len)
        score += 10 * len
        wordsFound++
        foundWordsList.add(pendingWord)
        pendingWord = ""
        wordLockTimer = 0f
    }

    private fun handleLetterCollision(letter: CollectableLetter) {
        letter.collected = true
        activeWord += letter.character
        checkWordValid()
    }

    private fun checkWordValid() {
        if (activeWord.isEmpty()) return
        val fullWord = pendingWord + activeWord

        var found = false
        for (lang in activeLanguages) {
            if (dictionaries[lang]?.contains(fullWord) == true) {
                found = true
                break
            }
        }

        if (found) {
            pendingWord = fullWord
            activeWord = ""
            wordLockTimer = 15.0f
            for (lang in activeLanguages) {
                if (dictionaries[lang]?.contains(fullWord) == true) {
                    currentLanguage = lang
                    break
                }
            }
        } else {
            var isPrefix = false
            for (lang in activeLanguages) {
                val dict = dictionaries[lang] ?: continue
                for (word in dict) {
                    if (word.startsWith(fullWord)) {
                        isPrefix = true
                        currentLanguage = lang
                        break
                    }
                }
                if (isPrefix) break
            }
            if (isPrefix) {
                if (wordLockTimer > 0) {
                    wordLockTimer = 15.0f
                }
            } else {
                if (wordLockTimer > 0 && pendingWord.isNotEmpty()) {
                    scorePendingWord()
                }

                var newPrefix = false
                for (lang in activeLanguages) {
                    val dict = dictionaries[lang] ?: continue
                    for (word in dict) {
                        if (word.startsWith(activeWord)) {
                            newPrefix = true
                            currentLanguage = lang
                            break
                        }
                    }
                    if (newPrefix) break
                }
                if (!newPrefix) {
                    activeWord = ""
                }
            }
        }
    }

    fun getWordLanguages(word: String): List<String> {
        return activeLanguages.filter { lang -> dictionaries[lang]?.contains(word) == true }
    }
}
