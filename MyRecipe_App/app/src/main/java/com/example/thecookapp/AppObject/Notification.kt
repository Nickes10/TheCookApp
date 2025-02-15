package com.example.thecookapp.AppObject

class Notification {
    private var userid:String=""
    private var text:String=""
    private var postUrl:String=""
    private var isLikeNotification:Boolean=false

    constructor()

    constructor(userid: String,text: String,postUrl:String, isLikedNotification:Boolean) {
        this.userid=userid
        this.text=text
        this.postUrl=postUrl
        this.isLikeNotification= isLikedNotification
    }

    fun getPostUrl():String{
        return postUrl
    }

    fun getUserId():String{
        return userid
    }
    fun getText():String{
        return text
    }
    fun getIsLikeNotification():Boolean{
        return isLikeNotification
    }

    fun setPostUrl(postUrl: String){
        this.postUrl= postUrl
    }

    fun setUserId(userid: String){
        this.userid= userid
    }

    fun setText(text: String){
        this.text= text
    }

    fun setIsLikeNotification(isLikedNotification: Boolean){
        this.isLikeNotification= isLikedNotification
    }
}