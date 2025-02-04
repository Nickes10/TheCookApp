package com.example.thecookapp

import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.thecookapp.AppObject.Notification
import com.example.thecookapp.R.color.colorApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    fun handleFollowButtonClick(context: Context, currentUserId: String, targetUserId: String, followButton: Button, onComplete: (() -> Unit)? = null) {
        // Function called when user press a 'Follow'/'Following' button
        val followRef = database.child("Follow")

        if (followButton.text.toString() == "Follow") {
            // Change text & background immediately for better UX
            followButton.text = "Following"
            followButton.setBackgroundResource(R.drawable.buttons_background_only_border)
            followButton.setTextColor(ContextCompat.getColor(context, colorApp))
            // Add to Following list
            followRef.child(currentUserId)
                .child("Following")
                .child(targetUserId)
                .setValue(true)

            // Add to Followers list
            followRef.child(targetUserId)
                .child("Followers")
                .child(currentUserId)
                .setValue(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Send a notification to the target user
                        pushNotification(targetUserId, isLikeNotification = false)
                        onComplete?.invoke()
                    }
                }

        } else if (followButton.text.toString() == "Following") {
            // Change text & background immediately for better UX
            followButton.text = "Follow"
            followButton.setBackgroundResource(R.drawable.buttons_background22)
            followButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))

            // Remove from Following list
            followRef.child(currentUserId)
                .child("Following")
                .child(targetUserId)
                .removeValue()

            // Remove from Followers list
            followRef.child(targetUserId)
                .child("Followers")
                .child(currentUserId)
                .removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Remove the notification to the target user
                        removeNotification(targetUserId, creatorNotification = currentUserId, isLikeNotification = false)
                        onComplete?.invoke()
                    }
                }
        }
    }

    fun checkFollowingStatus(context: Context, currentUserId: String, targetUserId: String, followButton: Button) {
        // Function to understand if the currentUser is following or not the targetUser
        val followingRef = database.child("Follow")
            .child(currentUserId)
            .child("Following")

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(datasnapshot: DataSnapshot) {
                if (datasnapshot.child(targetUserId).exists()) {
                    followButton.text = "Following"
                    followButton.setBackgroundResource(R.drawable.buttons_background_only_border)
                    followButton.setTextColor(ContextCompat.getColor(context, colorApp))
                } else {
                    followButton.text = "Follow"
                    followButton.setBackgroundResource(R.drawable.buttons_background22)
                    followButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        })
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

