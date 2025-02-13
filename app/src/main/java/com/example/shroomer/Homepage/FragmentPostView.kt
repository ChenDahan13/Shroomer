package com.example.shroomer.Homepage

import android.content.ComponentCallbacks
import android.widget.ArrayAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.shroomer.Entities.Comment
import com.example.shroomer.Entities.User
import com.example.shroomer.R
import com.example.shroomer.databinding.FragmentPostViewBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.util.LinkedList

class commentAdapter(context: Context, private val commentList: List<Comment>, val myUser_id: String) : ArrayAdapter<Comment>(context, 0, commentList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        }
        val currentComment = commentList[position]

        // Bind data to views in the layout content
        val commentContent = itemView?.findViewById<TextView>(R.id.comment_contents)
        commentContent?.text = currentComment.getContent()
        // Bind data to views in the layout owner
        val commentOwner = itemView?.findViewById<TextView>(R.id.username_who_commented)
        getUsername(currentComment.getUserId()) { username ->
            commentOwner?.text = username
        }

        // Set the comment ID as the tag for the like icon
        val likeIcon = itemView?.findViewById<TextView>(R.id.number_of_likes)
        likeIcon?.tag = currentComment.getCommentId()
        fetchNumberOfLikes(currentComment.getCommentId()) { numberOfLikes ->
            likeIcon?.text = numberOfLikes.toString()
        }
        likeIcon!!.setOnClickListener {
            isExpert(myUser_id) { isExpert ->
                if (isExpert) {
                    incrementLikes(it, myUser_id)
                    fetchNumberOfLikes(currentComment.getCommentId()) { numberOfLikes ->
                        likeIcon.text = numberOfLikes.toString()
                    }
                } else {
                    Toast.makeText(context, "Amateurs can't like comments", Toast.LENGTH_SHORT).show()
                }
            }
        }


        return itemView!!
    }
    private fun fetchNumberOfLikes(commentId: String, callback: (Int) -> Unit) {
        val likesRef = FirebaseDatabase.getInstance().getReference("Comment").child(commentId).child("likes")
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val numberOfLikes = snapshot.childrenCount.toInt() // Get the count of children under "likes"
                callback(numberOfLikes)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Log.e("fetchNumberOfLikes", "Error fetching number of likes: ${error.message}")
            }
        })
    }
    private fun isExpert(user_id: String, callback: (Boolean) -> Unit) {
        val databaseReferenceA = FirebaseDatabase.getInstance().reference.child("Amateur")
        val databaseReferenceE = FirebaseDatabase.getInstance().reference.child("Expert")

        databaseReferenceA.orderByChild("user_id").equalTo(user_id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        callback(false)
                    } else {
                        databaseReferenceE.orderByChild("user_id").equalTo(user_id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        callback(true)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Failed to read value
                                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun incrementLikes(view: View, myUser_id: String) {
        try {
            val comment_id = view.tag.toString()
            val commentRef = FirebaseDatabase.getInstance().getReference("Comment").child(comment_id).child("likes").child(myUser_id)
            // Check if the user has already liked the comment
            commentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User has already liked the comment, so remove the like
                        commentRef.removeValue()
                            .addOnSuccessListener {
                                Log.d("incrementLikes", "Like removed successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("incrementLikes", "Error removing like: ${e.message}")
                            }
                    } else {
                        // User hasn't liked the comment, so add the like
                        commentRef.setValue(true)
                            .addOnSuccessListener {
                                Log.d("incrementLikes", "Like added successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("incrementLikes", "Error adding like: ${e.message}")
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("incrementLikes", "Error checking like status: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("incrementLikes", "Error updating database: ${e.message}")
        }
    }

    private fun getUsername(user_id: CharSequence, callback: (String) -> Unit) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReferenceA = firebaseDatabase.reference.child("Amateur")
        val databaseReferenceE = firebaseDatabase.reference.child("Expert")

        databaseReferenceA.orderByChild("user_id").equalTo(user_id.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (amateurSnapshot in snapshot.children) {
                            val username = amateurSnapshot.child("username").getValue(String::class.java).toString()
                            callback(username)
                            return
                        }
                    } else {
                        databaseReferenceE.orderByChild("user_id").equalTo(user_id.toString())
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (expertSnapshot in snapshot.children) {
                                            val username = expertSnapshot.child("username").getValue(String::class.java).toString()
                                            callback(username)
                                            return
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Failed to read value
                                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                }
            })
    }


}

class FragmentPostView : Fragment() {

    private var _binding: FragmentPostViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var postID: String
    private lateinit var userOfPostID: String
    private var isAmateur: Boolean = false // True if the user is an amateur, false if the user is an expert

    // Firebase references
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReferencePost: DatabaseReference
    private lateinit var databaseReferenceComment: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            postID = it.getString("post_id").toString()
            userOfPostID = it.getString("user_id").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPostViewBinding.inflate(inflater, container, false)

        // Create the database reference
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReferencePost = firebaseDatabase.reference.child("Post")
        databaseReferenceComment = firebaseDatabase.reference.child("Comment")

        val myUser_id = activity?.intent?.getStringExtra("my_user_id")

        typeUserCheck(myUser_id.toString()) // Check if the user is an amateur or an expert
        viewPost()
        showComments(myUser_id!!)

        binding.submitCommentButton.setOnClickListener {
            if (isAmateur) {
                Toast.makeText(context, "Amateurs can't comment", Toast.LENGTH_SHORT).show()
            } else {
                val comment = binding.commentInput.text.toString()
                if (comment.isNotEmpty()) {
                    addComment(comment, myUser_id)
                } else {
                    Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return binding.root
    }

    // Check if the user is an amateur or an expert
    private fun typeUserCheck(myUser_id: String) {
        val databaseReferenceA = firebaseDatabase.reference.child("Amateur")

        databaseReferenceA.orderByChild("user_id").equalTo(myUser_id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        isAmateur = true
                    } else {
                        isAmateur = false
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Show the comment of the post
    private fun showComments(myUser_id: String) {
        databaseReferenceComment.orderByChild("post_id").equalTo(postID)
            .addValueEventListener(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val commentList = mutableListOf<Comment>()
                    for (commentSnapshot in snapshot.children) {
                        val content = commentSnapshot.child("content").getValue(String::class.java)
                        val user_id = commentSnapshot.child("user_id").getValue(String::class.java)
                        val comment_id =
                            commentSnapshot.child("comment_id").getValue(String::class.java)
                        val post_id = commentSnapshot.child("post_id").getValue(String::class.java)
                        val comment =
                            Comment(comment_id ?: "", content ?: "", user_id ?: "", post_id ?: "")
                        comment?.let {
                            commentList.add(it)
                        }
                    }
                    updateAdapter(commentList, myUser_id)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Toast.makeText(context, "Failed to read comments", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Show the post content
    private fun viewPost() {
        databaseReferencePost.orderByChild("post_id").equalTo(postID).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    val title = postSnapshot.child("title").getValue(String::class.java)
                    val userId = postSnapshot.child("user_id").getValue(String::class.java)
                    val imageUrl = postSnapshot.child("imageBitmap").getValue(String::class.java)

                    // Set the post content
                    binding.postTitleView.text = title
                    // Set the post owner
                    getUsername(userId.toString()) { username ->
                        binding.postOwnerUsername.text = username
                    }
                    // Load image
                    loadImageFromUrl(imageUrl)

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(context, "Failed to read post", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to load image from URL
    private fun loadImageFromUrl(imageUrl: String?) {
        Picasso.get().load(imageUrl).into(binding.postImageView)
    }

    // Add a comment to the post
    private fun addComment(comment: String, myUser_id: String?) {
        val commentID = databaseReferenceComment.push().key.toString() // Generate a unique key for the comment
        val user_id_comment_creator = myUser_id
        if (user_id_comment_creator == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val newComment = Comment(commentID, comment, user_id_comment_creator, postID)
        databaseReferenceComment.child(commentID).setValue(newComment.toMap())
            .addOnSuccessListener {
                Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    "Failed to add comment",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getUsername(user_id: String, callback: (String) -> Unit) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReferenceA = firebaseDatabase.reference.child("Amateur")
        val databaseReferenceE = firebaseDatabase.reference.child("Expert")

        databaseReferenceA.orderByChild("user_id").equalTo(user_id.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (amateurSnapshot in snapshot.children) {
                            val username = amateurSnapshot.child("username").getValue(String::class.java).toString()
                            callback(username)
                            return
                        }
                    } else {
                        databaseReferenceE.orderByChild("user_id").equalTo(user_id.toString())
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (expertSnapshot in snapshot.children) {
                                            val username = expertSnapshot.child("username").getValue(String::class.java).toString()
                                            callback(username)
                                            return
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Failed to read value
                                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Toast.makeText(context, "Failed to read user", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Update the adapter with the comments
    private fun updateAdapter(commentList: List<Comment>, myUser_id: String?) {
        val adapter = commentAdapter(requireContext(), commentList, myUser_id!!)
        val listView = view?.findViewById<ListView>(R.id.comments_list_view)
        listView?.adapter = adapter
    }
}