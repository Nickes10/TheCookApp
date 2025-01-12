package com.example.thecookapp

import com.google.firebase.database.FirebaseDatabase

object FirebaseUtils {

    private val database = FirebaseDatabase.getInstance().reference

    fun fetchUsername(userId: String, callback: (String) -> Unit) {
        database.child("Users").child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("fullname").value.toString()
                callback(username)
            } else {
                callback("Unknown User")
            }
        }.addOnFailureListener {
            callback("Unknown User")
        }
    }

    fun fetchProfileImage(userId: String, callback: (String) -> Unit) {
        database.child("Users").child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val profileImageUrl = snapshot.child("image").value.toString()
                callback(profileImageUrl)
            } else {
                callback("")
            }
        }.addOnFailureListener {
            callback("")
        }
    }
}
