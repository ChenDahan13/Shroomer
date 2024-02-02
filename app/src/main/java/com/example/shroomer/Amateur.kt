package com.example.shroomer

import java.util.LinkedList

class Amateur(name: String, email: String, password: String, user_id: String) :
    User(name, email, password, user_id) {

        private var posts: LinkedList<Long>

    init {
        this.posts = LinkedList()
    }

    fun createPost(title: String) {}
    fun deletePost(post: Post) {}
    fun prevPosts(): LinkedList<Long> { return this.posts }
}