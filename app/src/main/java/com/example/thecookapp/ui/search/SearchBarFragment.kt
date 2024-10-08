package com.example.thecookapp.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Adapter.UserAdapter
import com.example.thecookapp.AppObject.User
import com.example.thecookapp.R
import com.example.thecookapp.R.id.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchBarFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var userAdapter: UserAdapter? = null
    private var listUsers: MutableList<User>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize the binding object
        // Use ViewBinding to inflate the layout
        val view = inflater.inflate(R.layout.fragment_search_bar, container, false)

        recyclerView = view.findViewById(search_bar_recycler_view)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        listUsers= ArrayList()
        userAdapter = context?.let{ UserAdapter(it, listUsers as ArrayList<User>, true)}
        recyclerView?.adapter = userAdapter

        val querySearch: EditText = view.findViewById(search_input)

        querySearch.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (querySearch.text.toString() != "") {
                    recyclerView?.visibility = View.VISIBLE
                    searchUser(s.toString().lowercase())
                }
                else{
                    // When user remove what searched for, all users in database displayed
                    getAllUsers()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        return  view
    }

    private fun searchUser(input:String) {
        // Function to search users in database based on input String
        val query=FirebaseDatabase.getInstance().reference
            .child("Users")
            .orderByChild("fullname")
            .startAt(input)
            .endAt(input + "\uf8ff")

        query.addValueEventListener(object:ValueEventListener
        {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"Could not read from Database",Toast.LENGTH_LONG).show()

            }
            override fun onDataChange(datasnapshot: DataSnapshot) {
                listUsers?.clear()
                for(snapshot in datasnapshot.children)
                {
                    //searching all users
                    val user=snapshot.getValue(User::class.java)
                    if(user!=null && checkNotCurrentUser(user))
                    {
                        listUsers?.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
        })
    }


    private fun getAllUsers()
    // Function to save all the users in the Database in the Array<User>
    {
        val usersDatabaseRef= FirebaseDatabase.getInstance().reference.child("Users")

        // Get the UID of the current user (the one doing the search)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        usersDatabaseRef.addValueEventListener(object: ValueEventListener
        {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listUsers?.clear()

                // Cycles on all registered users
                for (snapShot in dataSnapshot.children) {
                    // Convert Each Firebase DataSnapshot into a User Object
                    val user = snapShot.getValue(User::class.java)
                    if (user != null && checkNotCurrentUser(user)) {
                        listUsers?.add(user)
                    }

                }
                // Telling to RecyclerView adapter that the data has changed, so to refresh the display.
                userAdapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"Could not read from Database",Toast.LENGTH_LONG).show()
            }

        })
    }

    private fun checkNotCurrentUser(user: User): Boolean {
        // Get the UID of the current user (the one doing the search)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        return user.getUid() != currentUserId
    }


}