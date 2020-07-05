package com.xenous.athenekotlin.threads

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xenous.athenekotlin.data.Category
import com.xenous.athenekotlin.data.Word
import com.xenous.athenekotlin.utils.*
import java.lang.Exception


class ReadWordsThread {

    private companion object {
        const val TAG = "ReadWordsThread"
    }

    interface ReadWordsResultListener {
        fun onSuccess(wordsList: MutableList<Word>, categoriesList: MutableList<Category>)

        fun onFailure(exception: Exception) {}

        fun onCanceled(error: DatabaseError)
    }

    private var downloadWordsResultListener: ReadWordsResultListener? = null

    fun setDownloadWordResultListener(downloadWordsResultListener: ReadWordsResultListener) {
        this.downloadWordsResultListener = downloadWordsResultListener
    }

    fun run() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if(firebaseUser == null) {
            Log.d(TAG, "Firebase User is null")
            this.downloadWordsResultListener?.onFailure(Exception("Firebase User is null"))

            return
        }

        val database = FirebaseDatabase.getInstance()
        val reference = database.reference.child(USERS_REFERENCE).child(firebaseUser.uid).child(WORDS_REFERENCE)
        val categoryReference = FirebaseDatabase.getInstance().reference.child(firebaseUser.uid).child(CATEGORY_REFERENCE)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Starting downloading words")

                val wordsList = mutableListOf<Word>()
                val categoriesList = mutableListOf<Category>()

                for(wordSnapshot in snapshot.children) {
                    val word = Word(
                        wordSnapshot.child(NATIVE_WORD_DATABASE_KEY).value as String?,
                        wordSnapshot.child(LEARNING_WORD_DATABASE_KEY).value as String?,
                        wordSnapshot.child(WORD_CATEGORY_DATABASE_KEY).value as String?,
                        wordSnapshot.child(WORD_LAST_DATE_DATABASE_KEY).value as Long?,
                        wordSnapshot.child(WORD_LEVEL_DATABASE_KEY).value as Long?,
                        wordSnapshot.key
                    )

                    wordsList.add(word)
                }

                Log.d(TAG, "Words list size is ${wordsList.size}")
                Log.d(TAG, "Completely downloaded all words")
                Log.d(TAG,  "Starting downloading categories")

                categoryReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for(categorySnapshot in snapshot.children) {
                            val category = Category(
                                categorySnapshot.value as String,
                                categorySnapshot.key as String
                            )

                            categoriesList.add(category)
                        }

                        Log.d(TAG, "All categories downloaded completely")
                        Log.d(TAG, categoriesList.size.toString())

                       downloadWordsResultListener?.onSuccess(wordsList, categoriesList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d(TAG, "Error while downloading categories, aborting... Error is ${error.message}")

                        downloadWordsResultListener?.onCanceled(error)
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Error while downloading words, aborting... Error is ${error.message}")

               downloadWordsResultListener?.onCanceled(error)
            }
        })
    }
}