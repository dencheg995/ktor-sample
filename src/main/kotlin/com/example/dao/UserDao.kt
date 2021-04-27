package com.example.dao

import catalogservice.jooq.tables.references.USER_DATA
import com.example.model.User
import org.jooq.CloseableDSLContext
import org.jooq.Row


class UserDao(private val ctx: CloseableDSLContext) {

    fun getAll() = ctx.select()
        .from(USER_DATA)
        .fetch()

    fun getById(id: Long?): Row? = ctx.select()
        .from(USER_DATA)
        .where(USER_DATA.ID.eq(id?.toInt())).fetchOne()?.valuesRow()

    fun createUser(user: User) {
        ctx.insertInto(
            USER_DATA,
            USER_DATA.LOGIN, USER_DATA.PASSWORD, USER_DATA.AGE, USER_DATA.FULL_NAME
        )
            .values(user.login, user.password, user.age, user.fullName)
            .execute();
    }

    fun delete(id: Long?) {
        ctx.deleteFrom(USER_DATA)
            .where(USER_DATA.ID.eq(id?.toInt()))
            .execute()
    }
}
