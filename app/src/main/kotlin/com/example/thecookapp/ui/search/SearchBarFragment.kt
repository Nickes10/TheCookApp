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
                    createArrayUser(querySearch.text.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        return  view
    }

    private fun searchUser(input:String) {

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
                Log.e("CAVO", "è entrato in searchUser")
                for(snapshot in datasnapshot.children)
                {
                    //searching all users
                    val user=snapshot.getValue(User::class.java)
                    if(user!=null)
                    {
                        listUsers?.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
        })
    }


    private fun createArrayUser(searchText: String)
    // Function to save all the users in the Database in the Array<User>
    {
        val usersSearchRef= FirebaseDatabase.getInstance().reference.child("Users")
        usersSearchRef.addValueEventListener(object: ValueEventListener
        {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (searchText == "") {
                    Log.e("CAVO", "è entrato in createArrayUser")
                    listUsers?.clear()

                    // Cycles on all registered users
                    for (snapShot in dataSnapshot.children) {
                        // Convert Each Firebase DataSnapshot into a User Object
                        val user = snapShot.getValue(User::class.java)
                        if (user != null) {
                            listUsers?.add(user)
                        }

                    }
                    // Telling to RecyclerView adapter that the data has changed, so to refresh the display.
                    userAdapter?.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"Could not read from Database",Toast.LENGTH_LONG).show()
            }

        })
    }

}