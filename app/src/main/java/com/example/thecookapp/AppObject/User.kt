package com.example.thecookapp.AppObject

class User
{
    private var username: String = ""
    private var fullname: String = ""
    private var uid: String = ""
    private var bio: String = ""
    private var image: String = ""

    constructor()

    constructor(username: String, fullname: String, uid:String, bio: String, image:String) {
        this.username = username
        this.fullname = fullname
        this.uid = uid
        this.bio = bio
        this.image = image
    }

    // Getters and Setters
    fun getUsername(): String{
        return username
    }

    fun setUsername(username:String){
        this.username= username
    }

    fun getUid():String{
        return uid
    }
    fun setUid(uid:String){
        this.uid= uid
    }
    
    fun getFullname(): String{
        return fullname
    }

    fun setFullname(fullname:String){
        val words = fullname.split(" ") // Split the full name into words
        val formattedWords = words.map { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
        this.fullname = formattedWords.joinToString(" ")
    }

    fun getBio(): String{
        return bio
    }

    fun setBio(bio:String){
        this.bio= bio
    }

    fun getImage(): String{
        return image
    }

    fun setImage(image:String){
        this.image= image
    }
}