package com.trader.core.domain.model

data class Customer(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** زبون زائر — يُستخدم افتراضياً عند إضافة عملية بدون تحديد زبون */
val WALK_IN_CUSTOMER = Customer(id = -1L, name = "زبون زائر", phone = "")