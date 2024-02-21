package com.example.shroomer.Entities

import java.util.LinkedList

class Comment(
    private var comment_id: String,
    private var content: String,
    private var user_id: String,
    private var like_count: Int=0
) {
    private lateinit var likedBy: LinkedList<Long>

    init {
        this.likedBy = LinkedList()
    }

    fun Comment.addLike(user_id: Long){
        this.likedBy.add(user_id)
        this.like_count++
    }

    fun Comment.removeLike(user_id: Long){
        if(likedBy.contains(user_id)){
            this.like_count--
            this.likedBy.remove(user_id)
        }
    }
    open fun toMap(): Map<String, Any> {
        return mapOf(
            "comment_id" to this.comment_id,
            "content" to this.content,
            "user_id" to this.user_id,
        )
    }
}