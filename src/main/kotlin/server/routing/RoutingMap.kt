package server.routing

import db.*
import server.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.requestsMap() {
    get("/api/textures") {
        try {
            val textures = DBOperator.getAllTextures()
            call.response.status(HttpStatusCode.OK)
            call.respond(textures.map { mapOf("id" to it.id.toString(), "filepath" to it.pathToFile) })
            logger.info("Successful GET /api/textures request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/textures", e)
        }
    }

    get("/api/textures/{id}") {
        val textureID = call.parameters["id"]?.toUIntOrNull() ?: 0u
        try {
            val textureFile = File(
                DBOperator.getTextureByID(textureID)?.pathToFile
                    ?: throw IllegalArgumentException("Texture #$textureID does not exist")
            )
            call.response.status(HttpStatusCode.OK)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, textureFile.name)
                    .toString()
            )
            call.respondFile(textureFile)
            logger.info("Successful GET /api/textures/$textureID request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "/api/textures/$textureID", e)
        }
    }

    get("/api/tilesets") {
        try {
            val tilesets = DBOperator.getAllTilesets()
            call.response.status(HttpStatusCode.OK)
            call.respond(tilesets.map { mapOf("id" to it.id.toString(), "filepath" to it.pathToJson) })
            logger.info("Successful GET /api/tilesets request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/tilesets", e)
        }
    }

    get("/api/tilesets/{id}") {
        val tilesetID = call.parameters["id"]?.toUIntOrNull() ?: 0u
        try {
            val tilesetFile = File(
                DBOperator.getTilesetByID(tilesetID)?.pathToJson
                    ?: throw IllegalArgumentException("Tileset #$tilesetID does not exist")
            )
            call.response.status(HttpStatusCode.OK)
            call.respond(tilesetFile.readText())
            logger.info("Successful GET /api/tilesets/$tilesetID request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "/api/tilesets/$tilesetID", e)
        }
    }

    get("/api/maps") {
        try {
            val maps = DBOperator.getAllMaps()
            call.response.status(HttpStatusCode.OK)
            call.respond(maps.map { mapOf("id" to it.id.toString(), "filepath" to it.pathToJson) })
            logger.info("Successful GET /api/maps request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/maps", e)
        }
    }

    get("/api/maps/{id}") {
        val mapID = call.parameters["id"]?.toUIntOrNull() ?: 0u
        try {
            val mapFile = File(
                DBOperator.getMapByID(mapID)?.pathToJson
                    ?: throw IllegalArgumentException("Map #$mapID does not exist")
            )
            call.response.status(HttpStatusCode.OK)
            call.respond(mapFile.readText())
            logger.info("Successful GET /api/maps/$mapID request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "/api/maps/$mapID", e)
        }
    }
}
