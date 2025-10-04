package com.architect.kmpessentials.aliases

typealias DefaultAction = () -> Unit
typealias DefaultActionAsync = suspend () -> Unit
typealias DefaultActionWithBooleanReturn = () -> Boolean
typealias DefaultActionWithBooleanReturnAsync = suspend () -> Boolean

typealias DefaultIntAction = (Int) -> Unit
typealias DefaultStringAction = (String) -> Unit
typealias DefaultBoolAction = (Boolean) -> Unit
typealias DefaultDoubleAction = (Double) -> Unit
typealias DefaultExceptionAction = (Throwable) -> Unit
typealias DefaultThrowableAction = (Throwable) -> Unit