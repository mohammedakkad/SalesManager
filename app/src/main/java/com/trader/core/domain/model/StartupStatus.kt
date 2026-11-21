package com.trader.core.domain.model


enum class StartupStatus {
    ACTIVE,     // all good — enter app
    DISABLED,   // blocked by admin
    EXPIRED,    // subscription ended
    DELETED,    // code no longer exists
    NOT_ACTIVATED, // never activated
    OFFLINE     // no internet — allow entry with warning
}