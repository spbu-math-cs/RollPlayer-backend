package server.routing

import db.DBOperator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import org.json.JSONObject
import server.logger
import server.utils.handleHTTPRequestException
import java.io.File
import java.time.LocalDateTime

fun Route.requestsPictures() {
    get("/api/pictures") {
        try {
            val pictures = DBOperator.getAllPictures()
            call.respond(
                HttpStatusCode.OK, JSONObject()
                    .put("type", "ok")
                    .put("result", pictures.map { mapOf("id" to it.id.toString(), "filepath" to it.pathToFile) })
                    .toString()
            )
            logger.info("Successful GET /api/pictures request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/pictures", e)
        }
    }

    get("/api/pictures/{id}") {
        val pictureID = call.parameters["id"]?.toUIntOrNull() ?: 0u
        try {
            val pictureFile = File(
                DBOperator.getPictureByID(pictureID)?.pathToFile
                    ?: throw IllegalArgumentException("Picture #$pictureID does not exist")
            )
            call.response.status(HttpStatusCode.OK)
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, pictureFile.name)
                    .toString()
            )
            call.respondFile(pictureFile)

            logger.info("Successful GET /api/pictures/$pictureID request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/pictures/$pictureID", e)
        }
    }

    /*
    post("/api/pictures") {
        try {
            val dateTime = LocalDateTime.now().toString()
            val filePath = "./resources/pictures/img_$dateTime.png"
            val file = File(filePath)
            call.receiveChannel().copyAndClose(file.writeChannel())

            val pictureInfo = DBOperator.addPicture(filePath)

            call.respond(
                HttpStatusCode.OK, JSONObject()
                    .put("type", "ok")
                    .put("result", mapOf("id" to pictureInfo.id.toString(), "filepath" to pictureInfo.pathToFile))
                    .toString()
            )

            logger.info("Successful POST /api/pictures request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "POST /api/pictures", e)
        }
    }
     */
}
