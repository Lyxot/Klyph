package xyz.hyli.klyph

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual val httpClient = HttpClient(Js)