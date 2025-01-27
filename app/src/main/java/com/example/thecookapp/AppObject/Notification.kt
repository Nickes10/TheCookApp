package com.example.thecookapp.AppObject

class Notification {
    private var userid:String=""
    private var text:String=""
    private var postid:String=""
    private var isLikedNotification:Boolean=false

    constructor()

    constructor(userid: String,text: String,postid:String, isLikedNotification:Boolean) {
        this.userid=userid
        this.text=text
        this.postid=postid
        this.isLikedNotification= isLikedNotification
    }

    fun getPostId():String{
        return postid
    }

    fun getUserId():String{
        return userid
    }
    fun getText():String{
        return text
    }
    fun getIsLikedNotification():Boolean{
        return isLikedNotification
    }

    fun setPostId(postid: String){
        this.postid= postid
    }

    fun setUserId(userid: String){
        this.userid= userid
    }

    fun setText(text: String){
        this.text= text
    }

    fun setIsLikedNotification(isLikedNotification: Boolean){
        this.isLikedNotification= isLikedNotification
    }
}