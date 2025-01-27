package com.example.thecookapp

import com.google.firebase.auth.FirebaseAuth
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

    fun pushNotification(userId: String, postId: String ="", isLikeNotification: Boolean) {
        // Function to push notification
        val ref = database.child("Notification").child(userId)

        val notifyMap = HashMap<String, Any>()
        notifyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
        notifyMap["postid"] = postId
        notifyMap["isLikeNotification"] = isLikeNotification

        if (isLikeNotification){
            notifyMap["text"] = "liked your post"
        } else{
            notifyMap["text"] = "started following you"
        }

        ref.push().setValue(notifyMap)
    }
}

