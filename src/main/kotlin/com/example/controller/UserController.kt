package com.example.controller

import com.example.dao.UserDao
import com.example.model.User
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting(userDao: UserDao) {
    routing {
        get("/{id}") {
            val id = call.parameters["id"]?.toLong()
            call.respondText(userDao.getById(id).toString())
        }

        get("/") {
            call.respondText(userDao.getAll().toString())
        }

        post("/") {
            val user = call.receive<User>()
            userDao.createUser(user)
            call.respondText("user with login ${user.login} was created")
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLong()
            userDao.delete(id)
            call.respondText("user with id = $id deleted")
        }
    }
}



