package com.henriquesebastiao.prumo.core.model

/** What a call to the server gave: the [Ok] value or the [Failed] reason. */
sealed interface Outcome<out T> {
    data class Ok<out T>(val value: T) : Outcome<T>

    data class Failed(val error: AppError) : Outcome<Nothing>
}

/** The value, or null when the call failed. */
fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Ok)?.value

/** The error, or null when the call worked. */
fun Outcome<*>.errorOrNull(): AppError? = (this as? Outcome.Failed)?.error

/** [transform] applied to the value; a failure passes through. */
inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Ok -> Outcome.Ok(transform(value))
    is Outcome.Failed -> this
}

/** Runs [action] with the value when the call worked; returns the outcome unchanged. */
inline fun <T> Outcome<T>.onOk(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Ok) action(value)
    return this
}

/** Runs [action] with the error when the call failed; returns the outcome unchanged. */
inline fun <T> Outcome<T>.onFailed(action: (AppError) -> Unit): Outcome<T> {
    if (this is Outcome.Failed) action(error)
    return this
}
