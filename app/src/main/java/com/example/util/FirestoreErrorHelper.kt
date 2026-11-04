package com.example.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

object FirestoreErrorHelper {

    fun handleFirestoreError(
        exception: Exception,
        operationType: OperationType,
        path: String
    ) {
        val code = if (exception is FirebaseFirestoreException) exception.code.name else "UNKNOWN"
        val message = exception.message ?: "No error message"
        Log.e(
            "FirestoreError",
            "{\"operation\": \"$operationType\", \"path\": \"$path\", \"code\": \"$code\", \"message\": \"$message\"}",
            exception
        )
    }
}
