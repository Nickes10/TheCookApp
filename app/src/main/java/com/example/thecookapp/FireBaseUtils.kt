package com.example.thecookapp

import android.util.Log
import android.widget.Toast
import com.example.thecookapp.AppObject.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

    fun pushNotification(userId: String, postUrl: String ="", isLikeNotification: Boolean) {
        // Function to push notification
        val ref = database.child("Notification").child(userId)

        val notifyMap = HashMap<String, Any>()
        notifyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
        notifyMap["postUrl"] = postUrl
        notifyMap["isLikeNotification"] = isLikeNotification

        if (isLikeNotification){
            notifyMap["text"] = "liked your recipe"
        } else{
            notifyMap["text"] = "started following you"
        }

        ref.push().setValue(notifyMap)
    }

    fun removeNotification(userId: String, postUrl: String = "", creatorNotification: String = "", isLikeNotification: Boolean) {
        // Function to remove notification
        val ref = database.child("Notification").child(userId)

        ref.orderByChild("userid").equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(Notification::class.java)

                        if (notification != null) {
                            if (isLikeNotification && notification.getPostUrl() == postUrl) {
                                // Remove only the like notification for this post
                                notificationSnapshot.ref.removeValue()
                            } else if (!isLikeNotification && notification.getUserId() == creatorNotification) {
                                // Remove only the follow notification
                                notificationSnapshot.ref.removeValue()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("removeNotification", "Error removing notification: ${error.message}")
                }
            })
    }

}

